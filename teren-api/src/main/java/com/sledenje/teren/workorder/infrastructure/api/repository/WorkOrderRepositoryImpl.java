package com.sledenje.teren.workorder.infrastructure.api.repository;

import com.sledenje.teren.workorder.domain.model.CreatedWorkOrder;
import com.sledenje.teren.workorder.domain.model.DeviceModelEntry;
import com.sledenje.teren.workorder.domain.model.PhotoEntry;
import com.sledenje.teren.workorder.domain.model.ValueChange;
import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderAttachments;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetails;
import com.sledenje.teren.workorder.domain.model.WorkOrderSearch;
import com.sledenje.teren.workorder.domain.model.WorkOrderSortField;
import com.sledenje.teren.workorder.domain.repository.WorkOrderRepository;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderClientJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderDeviceModelJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderEditJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderInstallerJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderJpaRepository;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderOptionJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderPhotoJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderUserJpaEntity;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class WorkOrderRepositoryImpl implements WorkOrderRepository {

    private static final char LIKE_ESCAPE = '\\';

    private final WorkOrderJpaRepository workOrderJpaRepository;
    private final JsonMapper jsonMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public WorkOrderRepositoryImpl(WorkOrderJpaRepository workOrderJpaRepository, JsonMapper jsonMapper) {
        this.workOrderJpaRepository = workOrderJpaRepository;
        this.jsonMapper = jsonMapper;
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

    @Override
    public boolean existsClient(String clientId) {
        return entityManager.find(WorkOrderClientJpaEntity.class, clientId) != null;
    }

    @Override
    public boolean existsUser(String userId) {
        return entityManager.find(WorkOrderUserJpaEntity.class, userId) != null;
    }

    @Override
    public int nextDailySequence(LocalDate day) {
        // En SQL stavek: hkratna vnosa istega dne dobita različni številki.
        Number lastNumber = (Number) entityManager.createNativeQuery("""
                        INSERT INTO "daily_sequence" ("orderDate", "lastNumber")
                        VALUES (:day, 1)
                        ON CONFLICT ("orderDate")
                        DO UPDATE SET "lastNumber" = "daily_sequence"."lastNumber" + 1
                        RETURNING "lastNumber"
                        """)
                .setParameter("day", day)
                .getSingleResult();
        return lastNumber.intValue();
    }

    @Override
    public CreatedWorkOrder saveNewWorkOrder(String ident, Instant orderDate, WorkOrderDetails details,
            List<DeviceModelEntry> deviceModels, String createdById) {
        WorkOrderJpaEntity entity = new WorkOrderJpaEntity(newId(), ident, orderDate,
                entityManager.getReference(WorkOrderUserJpaEntity.class, createdById));
        applyDetails(entity, details);
        deviceModels.forEach(deviceModel -> entity.getDeviceModels().add(new WorkOrderDeviceModelJpaEntity(
                newId(), entity, deviceModel.deviceModel(), deviceModel.comment())));
        WorkOrderJpaEntity savedEntity = workOrderJpaRepository.save(entity);
        return new CreatedWorkOrder(savedEntity.getId(), savedEntity.getIdent());
    }

    @Override
    public void updateWorkOrder(String workOrderId, WorkOrderDetails details, String editedById, Instant editedAt,
            Map<String, ValueChange> changes) {
        WorkOrderJpaEntity entity = workOrderJpaRepository.getReferenceById(workOrderId);
        // Najprej izbriši stare monterje in opcije, da nove iste vrste ne kršijo unikatnosti (nalog, opcija).
        entity.getInstallers().clear();
        entity.getOptions().clear();
        entityManager.flush();
        applyDetails(entity, details);
        entity.getEdits().add(new WorkOrderEditJpaEntity(newId(), entity, editedAt,
                entityManager.getReference(WorkOrderUserJpaEntity.class, editedById),
                jsonMapper.writeValueAsString(changes)));
    }

    @Override
    public boolean addAttachments(String workOrderId, WorkOrderAttachments attachments, Instant addedAt) {
        return workOrderJpaRepository.findById(workOrderId).map(entity -> {
            if (attachments.signatureUrl() != null) {
                entity.sign(attachments.signatureUrl(), addedAt);
            }
            for (PhotoEntry photo : attachments.photos()) {
                entity.getPhotos().add(new WorkOrderPhotoJpaEntity(
                        newId(), entity, photo.filePath(), photo.takenAt(), addedAt));
            }
            return true;
        }).orElse(false);
    }

    @Override
    public boolean markSent(String workOrderId, Instant sentAt) {
        return workOrderJpaRepository.findById(workOrderId).map(entity -> {
            entity.markSent(sentAt);
            return true;
        }).orElse(false);
    }

    private void applyDetails(WorkOrderJpaEntity entity, WorkOrderDetails details) {
        entity.updateDetails(
                details.type(),
                details.difficulty(),
                entityManager.getReference(WorkOrderClientJpaEntity.class, details.clientId()),
                details.vehiclePlate(),
                details.vehicleBrand(),
                details.vehicleModel(),
                details.vehicleYear(),
                details.imei(),
                details.imeiPrev(),
                details.culprit(),
                details.comment());
        details.installers().forEach(installer -> entity.getInstallers().add(new WorkOrderInstallerJpaEntity(
                newId(), entity, installer.name(), installer.otherText())));
        details.options().forEach(option -> entity.getOptions().add(new WorkOrderOptionJpaEntity(
                newId(), entity, option.optionType(), option.comment())));
    }

    private static String newId() {
        // Obstoječi zapisi imajo cuid ID-je (Prisma), novi dobijo UUID; stolpec je TEXT in format ni preverjan.
        return UUID.randomUUID().toString();
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
