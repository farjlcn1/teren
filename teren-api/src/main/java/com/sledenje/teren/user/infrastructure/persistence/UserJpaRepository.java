package com.sledenje.teren.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    List<UserJpaEntity> findAllByOrderByCreatedAtAsc();

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    // Tuji ključi iz nalogov, urejanj in plana imajo ON DELETE RESTRICT; preverimo vnaprej za jasno sporočilo.
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM "work_orders" WHERE "createdById" = :userId)
                OR EXISTS (SELECT 1 FROM "work_order_edits" WHERE "editedById" = :userId)
                OR EXISTS (SELECT 1 FROM "plan_groups" WHERE "createdById" = :userId)
            """, nativeQuery = true)
    boolean isReferencedByWorkOrdersEditsOrPlanGroups(@Param("userId") String userId);

}
