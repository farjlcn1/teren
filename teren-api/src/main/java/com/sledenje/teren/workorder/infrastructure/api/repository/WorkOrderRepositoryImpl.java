package com.sledenje.teren.workorder.infrastructure.api.repository;

import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderSearch;
import com.sledenje.teren.workorder.domain.model.WorkOrderSortField;
import com.sledenje.teren.workorder.domain.repository.WorkOrderRepository;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderClientJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderJpaRepository;
import com.sledenje.teren.workorder.infrastructure.util.WorkOrderJpaConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WorkOrderRepositoryImpl implements WorkOrderRepository {

    private static final char LIKE_ESCAPE = '\\';

    private final WorkOrderJpaRepository workOrderJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public WorkOrderRepositoryImpl(WorkOrderJpaRepository workOrderJpaRepository) {
        this.workOrderJpaRepository = workOrderJpaRepository;
    }

    @Override
    public List<WorkOrder> findWorkOrders(WorkOrderSearch search) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<WorkOrderJpaEntity> query = criteriaBuilder.createQuery(WorkOrderJpaEntity.class);
        Root<WorkOrderJpaEntity> workOrder = query.from(WorkOrderJpaEntity.class);
        // Stranka in avtor se naložita v isti poizvedbi; stranka je potrebna tudi za urejanje po imenu.
        @SuppressWarnings("unchecked")
        Join<WorkOrderJpaEntity, WorkOrderClientJpaEntity> client =
                (Join<WorkOrderJpaEntity, WorkOrderClientJpaEntity>) workOrder.<WorkOrderJpaEntity, WorkOrderClientJpaEntity>fetch("client");
        workOrder.fetch("createdBy");

        query.select(workOrder)
                .where(buildPredicates(search, criteriaBuilder, query, workOrder).toArray(Predicate[]::new))
                .orderBy(
                        search.ascending()
                                ? criteriaBuilder.asc(sortExpression(search.sortField(), workOrder, client))
                                : criteriaBuilder.desc(sortExpression(search.sortField(), workOrder, client)),
                        // Enak vrstni red pri enakih vrednostih (npr. isti datum).
                        criteriaBuilder.asc(workOrder.get("id")));

        TypedQuery<WorkOrderJpaEntity> typedQuery = entityManager.createQuery(query);
        if (search.limit() != null) {
            typedQuery.setMaxResults(search.limit());
        }
        return typedQuery.getResultList().stream().map(WorkOrderJpaConverter::toDomain).toList();
    }

    @Override
    public Optional<WorkOrderDetail> findWorkOrderDetail(String workOrderId) {
        return workOrderJpaRepository.findById(workOrderId).map(WorkOrderJpaConverter::toDetail);
    }

    @Override
    public List<String> findDistinctImeis() {
        return workOrderJpaRepository.findDistinctImeis();
    }

    private static List<Predicate> buildPredicates(WorkOrderSearch search, CriteriaBuilder criteriaBuilder,
            CriteriaQuery<?> query, Root<WorkOrderJpaEntity> workOrder) {
        List<Predicate> predicates = new ArrayList<>();
        if (search.orderDateFrom() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(workOrder.<Instant>get("orderDate"), search.orderDateFrom()));
        }
        if (search.orderDateTo() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(workOrder.<Instant>get("orderDate"), search.orderDateTo()));
        }
        if (search.clientId() != null) {
            predicates.add(criteriaBuilder.equal(workOrder.get("client").get("id"), search.clientId()));
        }
        if (search.createdById() != null) {
            predicates.add(criteriaBuilder.equal(workOrder.get("createdBy").get("id"), search.createdById()));
        }
        if (search.vehiclePlate() != null) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(workOrder.<String>get("vehiclePlate")),
                    containsPattern(search.vehiclePlate().toLowerCase(Locale.ROOT)), LIKE_ESCAPE));
        }
        if (search.imei() != null) {
            predicates.add(criteriaBuilder.like(workOrder.<String>get("imei"), containsPattern(search.imei()), LIKE_ESCAPE));
        }
        if (search.type() != null) {
            predicates.add(criteriaBuilder.equal(workOrder.get("type"), search.type()));
        }
        if (search.difficulty() != null) {
            predicates.add(criteriaBuilder.equal(workOrder.get("difficulty"), search.difficulty()));
        }
        if (search.culprit() != null) {
            predicates.add(criteriaBuilder.equal(workOrder.get("culprit"), search.culprit()));
        }
        if (search.installer() != null) {
            predicates.add(hasChild(criteriaBuilder, query, workOrder, "installers", "name", search.installer()));
        }
        if (search.option() != null) {
            predicates.add(hasChild(criteriaBuilder, query, workOrder, "options", "optionType", search.option()));
        }
        if (search.deviceModel() != null) {
            predicates.add(hasChild(criteriaBuilder, query, workOrder, "deviceModels", "deviceModel",
                    search.deviceModel()));
        }
        return predicates;
    }

    /**
     * EXISTS namesto JOIN, da se nalog z več ujemajočimi zapisi ne ponovi.
     */
    private static Predicate hasChild(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> query,
            Root<WorkOrderJpaEntity> workOrder, String collection, String attribute, Object value) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<WorkOrderJpaEntity> correlatedWorkOrder = subquery.correlate(workOrder);
        Join<WorkOrderJpaEntity, ?> child = correlatedWorkOrder.join(collection);
        subquery.select(criteriaBuilder.literal(1)).where(criteriaBuilder.equal(child.get(attribute), value));
        return criteriaBuilder.exists(subquery);
    }

    private static Expression<?> sortExpression(WorkOrderSortField sortField, Root<WorkOrderJpaEntity> workOrder,
            Join<WorkOrderJpaEntity, WorkOrderClientJpaEntity> client) {
        return switch (sortField) {
            case ORDER_DATE -> workOrder.get("orderDate");
            case IDENT -> workOrder.get("ident");
            case VEHICLE_PLATE -> workOrder.get("vehiclePlate");
            case IMEI -> workOrder.get("imei");
            case CLIENT_NAME -> client.get("name");
            case TYPE -> workOrder.get("type");
            case DIFFICULTY -> workOrder.get("difficulty");
            case CULPRIT -> workOrder.get("culprit");
        };
    }

    /**
     * Iskani niz se ujema dobesedno: % in _ v njem nista nadomestna znaka (kot "contains" v Prismi).
     */
    private static String containsPattern(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

}
