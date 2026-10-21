package com.sledenje.teren.plan.infrastructure.api.service;

import com.sledenje.teren.plan.domain.model.CompletedWorkOrder;
import com.sledenje.teren.plan.domain.model.NewPlanGroup;
import com.sledenje.teren.plan.domain.model.NewPlannedTask;
import com.sledenje.teren.plan.domain.model.PlanGroupWindow;
import com.sledenje.teren.plan.domain.repository.PlanGroupRepository;
import com.sledenje.teren.plan.domain.service.PlanService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    private final PlanGroupRepository planGroupRepository;

    public PlanServiceImpl(PlanGroupRepository planGroupRepository) {
        this.planGroupRepository = planGroupRepository;
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

}
