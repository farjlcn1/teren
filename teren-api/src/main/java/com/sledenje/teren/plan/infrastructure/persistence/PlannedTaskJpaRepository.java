package com.sledenje.teren.plan.infrastructure.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlannedTaskJpaRepository extends JpaRepository<PlannedTaskJpaEntity, String> {

    // En stavek: če je planirani nalog med tem že opravljen ali pripada drugi stranki, se ne posodobi nič.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PlannedTaskJpaEntity t
            SET t.workOrderId = :workOrderId, t.updatedAt = :now
            WHERE t.id = :plannedTaskId
              AND t.workOrderId IS NULL
              AND t.planGroup.id IN (SELECT g.id FROM PlanGroupJpaEntity g WHERE g.client.id = :clientId)
            """)
    int linkToWorkOrder(@Param("plannedTaskId") String plannedTaskId, @Param("workOrderId") String workOrderId,
            @Param("clientId") String clientId, @Param("now") Instant now);

}
