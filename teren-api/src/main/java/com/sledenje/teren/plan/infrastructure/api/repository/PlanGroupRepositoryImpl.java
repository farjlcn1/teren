package com.sledenje.teren.plan.infrastructure.api.repository;

import com.sledenje.teren.common.domain.model.InstallerName;
import com.sledenje.teren.plan.domain.model.NewPlanGroup;
import com.sledenje.teren.plan.domain.model.NewPlannedTask;
import com.sledenje.teren.plan.domain.model.PlanGroup;
import com.sledenje.teren.plan.domain.model.PlanGroupWindow;
import com.sledenje.teren.plan.domain.model.PlannedTaskDetail;
import com.sledenje.teren.plan.domain.repository.PlanGroupRepository;
import com.sledenje.teren.plan.infrastructure.persistence.PlanClientJpaEntity;
import com.sledenje.teren.plan.infrastructure.persistence.PlanGroupJpaEntity;
import com.sledenje.teren.plan.infrastructure.persistence.PlanGroupJpaRepository;
import com.sledenje.teren.plan.infrastructure.persistence.PlannedTaskJpaEntity;
import com.sledenje.teren.plan.infrastructure.persistence.PlannedTaskJpaRepository;
import com.sledenje.teren.plan.infrastructure.util.PlanJpaConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    public String savePlannedTask(String planGroupId, NewPlannedTask task) {
        return plannedTaskJpaRepository.save(toEntity(planGroupJpaRepository.getReferenceById(planGroupId), task))
                .getId();
    }

    @Override
    public List<PlanGroup> findPlanGroupsOverlapping(Instant from, Instant to) {
        return planGroupJpaRepository.findOverlapping(from, to).stream().map(PlanJpaConverter::toDomain).toList();
    }

    @Override
    public Optional<PlanGroup> findPlanGroup(String planGroupId) {
        return planGroupJpaRepository.findById(planGroupId).map(PlanJpaConverter::toDomain);
    }

    @Override
    public Optional<PlannedTaskDetail> findPlannedTask(String plannedTaskId) {
        return plannedTaskJpaRepository.findById(plannedTaskId).map(PlanJpaConverter::toDetail);
    }

    @Override
    public boolean existsClient(String clientId) {
        return entityManager.find(PlanClientJpaEntity.class, clientId) != null;
    }

    @Override
    public Optional<PlanGroupWindow> findFirstOverlappingGroupOfInstaller(InstallerName installer,
            boolean matchOtherText, String installerOtherText, Instant startAt, Instant endAt,
            Set<String> excludedIds) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PlanGroupJpaEntity> query = criteriaBuilder.createQuery(PlanGroupJpaEntity.class);
        Root<PlanGroupJpaEntity> planGroup = query.from(PlanGroupJpaEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(planGroup.get("expectedInstaller"), installer));
        if (matchOtherText) {
            predicates.add(installerOtherText == null
                    ? criteriaBuilder.isNull(planGroup.get("expectedInstallerOtherText"))
                    : criteriaBuilder.equal(planGroup.get("expectedInstallerOtherText"), installerOtherText));
        }
        predicates.add(criteriaBuilder.lessThan(planGroup.<Instant>get("startAt"), endAt));
        predicates.add(criteriaBuilder.greaterThan(planGroup.<Instant>get("endAt"), startAt));
        if (!excludedIds.isEmpty()) {
            predicates.add(criteriaBuilder.not(planGroup.get("id").in(excludedIds)));
        }
        query.select(planGroup)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(criteriaBuilder.asc(planGroup.get("startAt")));

        return entityManager.createQuery(query).setMaxResults(1).getResultStream()
                .findFirst()
                .map(entity -> new PlanGroupWindow(entity.getId(), entity.getStartAt(), entity.getEndAt()));
    }

    @Override
    public void updatePlanGroup(String planGroupId, NewPlanGroup planGroup) {
        planGroupJpaRepository.findById(planGroupId).ifPresent(entity -> entity.update(
                entityManager.getReference(PlanClientJpaEntity.class, planGroup.clientId()),
                planGroup.startAt(),
                planGroup.endAt(),
                planGroup.note(),
                planGroup.contact(),
                planGroup.expectedInstaller(),
                planGroup.expectedInstallerOtherText()));
    }

    @Override
    public void deletePlanGroup(String planGroupId) {
        planGroupJpaRepository.deleteById(planGroupId);
    }

    @Override
    public void deletePlannedTask(String plannedTaskId) {
        plannedTaskJpaRepository.findById(plannedTaskId).ifPresent(task -> {
            // Iz zbirke dogodka, da ga orphanRemoval ne poskusi znova shraniti.
            task.getPlanGroup().getTasks().remove(task);
            plannedTaskJpaRepository.delete(task);
        });
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
