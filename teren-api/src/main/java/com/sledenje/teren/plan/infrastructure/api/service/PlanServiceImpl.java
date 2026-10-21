package com.sledenje.teren.plan.infrastructure.api.service;

import com.sledenje.teren.common.domain.model.InstallerName;
import com.sledenje.teren.plan.domain.exception.InvalidPlanGroupException;
import com.sledenje.teren.plan.domain.exception.PlanGroupCompletedException;
import com.sledenje.teren.plan.domain.exception.PlanGroupNotFoundException;
import com.sledenje.teren.plan.domain.exception.PlannedTaskCompletedException;
import com.sledenje.teren.plan.domain.exception.PlannedTaskNotFoundException;
import com.sledenje.teren.plan.domain.model.CompletedWorkOrder;
import com.sledenje.teren.plan.domain.model.NewPlanGroup;
import com.sledenje.teren.plan.domain.model.NewPlannedTask;
import com.sledenje.teren.plan.domain.model.PlanGroup;
import com.sledenje.teren.plan.domain.model.PlanGroupChange;
import com.sledenje.teren.plan.domain.model.PlanGroupWindow;
import com.sledenje.teren.plan.domain.model.PlannedTaskDetail;
import com.sledenje.teren.plan.domain.repository.PlanGroupRepository;
import com.sledenje.teren.plan.domain.service.PlanService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanServiceImpl implements PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanServiceImpl.class);

    /**
     * Privzeto trajanje dogodka brez izbranega konca in dogodka, ustvarjenega iz naloga.
     */
    static final Duration DEFAULT_EVENT_DURATION = Duration.ofHours(1);

    /**
     * Varovalka pred neskončno verigo zamikov.
     */
    private static final int MAX_SHIFTED_EVENTS = 500;

    private final PlanGroupRepository planGroupRepository;

    public PlanServiceImpl(PlanGroupRepository planGroupRepository) {
        this.planGroupRepository = planGroupRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanGroup> findPlanGroups(Instant from, Instant to) {
        return planGroupRepository.findPlanGroupsOverlapping(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public PlannedTaskDetail findPlannedTask(String plannedTaskId) {
        return planGroupRepository.findPlannedTask(plannedTaskId).orElseThrow(PlannedTaskNotFoundException::new);
    }

    @Override
    @Transactional
    public String createPlanGroup(PlanGroupChange change, List<NewPlannedTask> tasks, String createdById) {
        Instant endAt = change.endAt() != null ? change.endAt() : change.startAt().plus(DEFAULT_EVENT_DURATION);
        NewPlanGroup planGroup = validated(change, endAt, createdById);
        for (NewPlannedTask task : tasks) {
            requireVehiclePlate(task);
        }

        pushOverlappingEventsOfInstaller(planGroup, Set.of());
        String planGroupId = planGroupRepository.savePlanGroup(planGroup, tasks);
        log.info("Ustvarjen dogodek {} z {} nalogi", planGroupId, tasks.size());
        return planGroupId;
    }

    @Override
    @Transactional
    public void updatePlanGroup(String planGroupId, PlanGroupChange change) {
        PlanGroup existing = planGroupRepository.findPlanGroup(planGroupId).orElseThrow(PlanGroupNotFoundException::new);
        if (existing.hasCompletedTask() && !existing.clientId().equals(change.clientId())) {
            throw new InvalidPlanGroupException(
                    "Stranke ni mogoče spremeniti, ker je bil vsaj en nalog v tej skupini že opravljen.");
        }
        // Pri urejanju je konec obvezen; privzeto trajanje velja samo za nov dogodek.
        if (change.endAt() == null) {
            throw new InvalidPlanGroupException("Vnesi konec.");
        }
        NewPlanGroup planGroup = validated(change, change.endAt(), existing.createdById());

        pushOverlappingEventsOfInstaller(planGroup, Set.of(planGroupId));
        planGroupRepository.updatePlanGroup(planGroupId, planGroup);
        log.info("Posodobljen dogodek {}", planGroupId);
    }

    @Override
    @Transactional
    public void movePlanGroup(String planGroupId, Instant startAt, Instant endAt) {
        PlanGroup existing = planGroupRepository.findPlanGroup(planGroupId).orElseThrow(PlanGroupNotFoundException::new);
        requireEndAfterStart(startAt, endAt);

        pushOverlappingEventsOfInstaller(existing.expectedInstaller(), existing.expectedInstallerOtherText(),
                startAt, endAt, Set.of(planGroupId));
        planGroupRepository.updatePlanGroupWindow(planGroupId, startAt, endAt);
        log.info("Premaknjen dogodek {}", planGroupId);
    }

    @Override
    @Transactional
    public void deletePlanGroup(String planGroupId) {
        PlanGroup existing = planGroupRepository.findPlanGroup(planGroupId).orElseThrow(PlanGroupNotFoundException::new);
        if (existing.hasCompletedTask()) {
            throw new PlanGroupCompletedException();
        }
        planGroupRepository.deletePlanGroup(planGroupId);
        log.info("Izbrisan dogodek {}", planGroupId);
    }

    @Override
    @Transactional
    public String addPlannedTask(String planGroupId, NewPlannedTask task) {
        planGroupRepository.findPlanGroup(planGroupId).orElseThrow(PlanGroupNotFoundException::new);
        requireVehiclePlate(task);
        return planGroupRepository.savePlannedTask(planGroupId, task);
    }

    @Override
    @Transactional
    public void deletePlannedTask(String plannedTaskId) {
        PlannedTaskDetail existing = planGroupRepository.findPlannedTask(plannedTaskId)
                .orElseThrow(PlannedTaskNotFoundException::new);
        if (existing.task().isCompleted()) {
            throw new PlannedTaskCompletedException();
        }
        planGroupRepository.deletePlannedTask(plannedTaskId);
    }

    @Override
    @Transactional
    public void recordCompletedWorkOrder(CompletedWorkOrder completed) {
        if (completed.plannedTaskId() != null) {
            boolean linked = planGroupRepository.linkPlannedTaskToWorkOrder(
                    completed.plannedTaskId(), completed.workOrderId(), completed.clientId());
            if (!linked) {
                // Nalog ostane veljaven; planirani nalog je med tem opravil nekdo drug ali ne pripada stranki.
                log.warn("Planirani nalog {} ni bil povezan z nalogom {}", completed.plannedTaskId(),
                        completed.workOrderId());
            }
            return;
        }

        NewPlannedTask completedTask = new NewPlannedTask(
                completed.vehiclePlate(), completed.type(), completed.note(), completed.workOrderId());
        Instant defaultEndAt = completed.completedAt().plus(DEFAULT_EVENT_DURATION);

        // Več nalogov istega monterja pri isti stranki zaporedoma pristane v istem dogodku.
        Optional<PlanGroupWindow> ongoingGroup = planGroupRepository.findLatestGroupOfInstallerEndingAfter(
                completed.clientId(), completed.installer(), completed.installerOtherText(), completed.completedAt());
        if (ongoingGroup.isPresent()) {
            PlanGroupWindow window = ongoingGroup.get();
            planGroupRepository.savePlannedTask(window.id(), completedTask);
            if (defaultEndAt.isAfter(window.endAt())) {
                planGroupRepository.updatePlanGroupWindow(window.id(), window.startAt(), defaultEndAt);
            }
            log.info("Nalog {} dodan k dogodku {}", completed.workOrderId(), window.id());
            return;
        }

        String planGroupId = planGroupRepository.savePlanGroup(
                new NewPlanGroup(completed.clientId(), completed.completedAt(), defaultEndAt, null, null,
                        completed.installer(), completed.installerOtherText(), completed.createdById()),
                List.of(completedTask));
        log.info("Za nalog {} ustvarjen dogodek {}", completed.workOrderId(), planGroupId);
    }

    /**
     * Preveri podatke iz obrazca; ime monterja ostane samo pri "Ostalo".
     */
    private NewPlanGroup validated(PlanGroupChange change, Instant endAt, String createdById) {
        requireEndAfterStart(change.startAt(), endAt);
        String otherText = change.expectedInstaller() == InstallerName.OSTALO
                ? blankToNull(change.expectedInstallerOtherText())
                : null;
        if (change.expectedInstaller() == InstallerName.OSTALO && otherText == null) {
            throw new InvalidPlanGroupException("Vnesi ime monterja pri izbiri 'Ostalo'.");
        }
        if (!planGroupRepository.existsClient(change.clientId())) {
            throw new InvalidPlanGroupException("Stranka ne obstaja.");
        }
        return new NewPlanGroup(change.clientId(), change.startAt(), endAt, blankToNull(change.note()),
                blankToNull(change.contact()), change.expectedInstaller(), otherText, createdById);
    }

    private void pushOverlappingEventsOfInstaller(NewPlanGroup planGroup, Set<String> excludedIds) {
        pushOverlappingEventsOfInstaller(planGroup.expectedInstaller(), planGroup.expectedInstallerOtherText(),
                planGroup.startAt(), planGroup.endAt(), excludedIds);
    }

    /**
     * Prekrivajoč dogodek istega monterja zamakne tik za konec okna, nato v zanki še vse naslednje, ki bi jih ta
     * zamik prekril. Brez monterja se ne zgodi nič. "Ostalo" se primerja tudi po imenu, ker gre za različne ljudi.
     */
    private void pushOverlappingEventsOfInstaller(InstallerName installer, String installerOtherText,
            Instant startAt, Instant endAt, Set<String> excludedIds) {
        if (installer == null) {
            return;
        }
        Instant cursorStart = startAt;
        Instant cursorEnd = endAt;
        Set<String> shiftedIds = new HashSet<>(excludedIds);
        for (int i = 0; i < MAX_SHIFTED_EVENTS; i++) {
            Optional<PlanGroupWindow> conflict = planGroupRepository.findFirstOverlappingGroupOfInstaller(
                    installer, installer == InstallerName.OSTALO, installerOtherText, cursorStart, cursorEnd,
                    shiftedIds);
            if (conflict.isEmpty()) {
                return;
            }
            Duration duration = Duration.between(conflict.get().startAt(), conflict.get().endAt());
            cursorStart = cursorEnd;
            cursorEnd = cursorStart.plus(duration);
            planGroupRepository.updatePlanGroupWindow(conflict.get().id(), cursorStart, cursorEnd);
            shiftedIds.add(conflict.get().id());
            log.info("Dogodek {} zamaknjen na {}", conflict.get().id(), cursorStart);
        }
    }

    private static void requireEndAfterStart(Instant startAt, Instant endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new InvalidPlanGroupException("Konec mora biti po začetku.");
        }
    }

    private static void requireVehiclePlate(NewPlannedTask task) {
        if (task.vehiclePlate() == null || task.vehiclePlate().isBlank()) {
            throw new InvalidPlanGroupException("Vnesi registrsko številko.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

}
