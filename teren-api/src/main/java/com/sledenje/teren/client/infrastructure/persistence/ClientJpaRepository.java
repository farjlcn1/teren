package com.sledenje.teren.client.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, String> {

    List<ClientJpaEntity> findAllByOrderByNameAsc();

    Optional<ClientJpaEntity> findByExternalId(String externalId);

    // Tuja ključa iz delovnih nalogov in plana imata ON DELETE RESTRICT; preverimo vnaprej za jasno sporočilo.
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM "work_orders" WHERE "clientId" = :clientId)
                OR EXISTS (SELECT 1 FROM "plan_groups" WHERE "clientId" = :clientId)
            """, nativeQuery = true)
    boolean isReferencedByWorkOrdersOrPlanGroups(@Param("clientId") String clientId);

}
