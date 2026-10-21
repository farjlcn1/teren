package com.sledenje.teren.plan.infrastructure.api.repository;

import com.sledenje.teren.common.domain.model.InstallerName;
import com.sledenje.teren.plan.domain.model.NewPlanGroup;
import com.sledenje.teren.plan.domain.model.NewPlannedTask;
import com.sledenje.teren.plan.domain.model.PlanGroupWindow;
import com.sledenje.teren.plan.domain.repository.PlanGroupRepository;
import com.sledenje.teren.plan.infrastructure.persistence.PlanClientJpaEntity;
import com.sledenje.teren.plan.infrastructure.persistence.PlanGroupJpaEntity;
import com.sledenje.teren.plan.infrastructure.persistence.PlanGroupJpaRepository;
import com.sledenje.teren.plan.infrastructure.persistence.PlannedTaskJpaEntity;
import com.sledenje.teren.plan.infrastructure.persistence.PlannedTaskJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

@Repository
public class PlanGroupRepositoryImpl implements PlanGroupRepository {

    private final PlanGroupJpaRepository planGroupJpaRepository;
    private final PlannedTaskJpaRepository plannedTaskJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public PlanGroupRepositoryImpl(PlanGroupJpaRepository planGroupJpaRepository,
            PlannedTaskJpaRepository plannedTaskJpaRepository) {
        this.planGroupJpaRepository = planGroupJpaRepository;
        this.plannedTaskJpaRepository = plannedTaskJpaRepository;
    }

    @Override
    public String savePlanGroup(NewPlanGroup planGroup, List<NewPlannedTask> tasks) {
        PlanGroupJpaEntity entity = new PlanGroupJpaEntity(
                newId(),
                entityManager.getReference(PlanClientJpaEntity.class, planGroup.clientId()),
                planGroup.startAt(),
                planGroup.endAt(),
                planGroup.note(),
                planGroup.contact(),
                planGroup.expectedInstaller(),
                planGroup.expectedInstallerOtherText(),
                planGroup.createdById());
        tasks.forEach(task -> entity.getTasks().add(toEntity(entity, task)));
        return planGroupJpaRepository.save(entity).getId();
    }

    @Override
    public void savePlannedTask(String planGroupId, NewPlannedTask task) {
        plannedTaskJpaRepository.save(toEntity(planGroupJpaRepository.getReferenceById(planGroupId), task));
    }

    @Override
    public void updatePlanGroupWindow(String planGroupId, Instant startAt, Instant endAt) {
        planGroupJpaRepository.findById(planGroupId).ifPresent(entity -> entity.changeWindow(startAt, endAt));
    }

    @Override
    public Optional<PlanGroupWindow> findLatestGroupOfInstallerEndingAfter(String clientId, InstallerName installer,
            String installerOtherText, Instant notEndingBefore) {
        return planGroupJpaRepository.findLatestOfInstallerEndingAfter(
                        clientId, installer, installerOtherText, notEndingBefore, Limit.of(1))
                .stream()
                .findFirst()
                .map(entity -> new PlanGroupWindow(entity.getId(), entity.getStartAt(), entity.getEndAt()));
    }

    @Override
    public boolean linkPlannedTaskToWorkOrder(String plannedTaskId, String workOrderId, String clientId) {
        return plannedTaskJpaRepository.linkToWorkOrder(plannedTaskId, workOrderId, clientId,
                Instant.now().truncatedTo(ChronoUnit.MILLIS)) > 0;
    }

    private static PlannedTaskJpaEntity toEntity(PlanGroupJpaEntity planGroup, NewPlannedTask task) {
        return new PlannedTaskJpaEntity(newId(), planGroup, task.vehiclePlate(), task.type(), task.note(),
                task.workOrderId());
    }

    private static String newId() {
        // Obstoječi zapisi imajo cuid ID-je (Prisma), novi dobijo UUID; stolpec je TEXT in format ni preverjan.
        return UUID.randomUUID().toString();
    }

}
