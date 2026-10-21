package com.sledenje.teren.plan.infrastructure.persistence;

import com.sledenje.teren.common.domain.model.InstallerName;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanGroupJpaRepository extends JpaRepository<PlanGroupJpaEntity, String> {

    // Prazno ime monterja (null) se ujema samo z dogodki brez imena, kot v Prismi.
    @Query("""
            SELECT g FROM PlanGroupJpaEntity g
            WHERE g.client.id = :clientId
              AND g.expectedInstaller = :installer
              AND ((:otherText IS NULL AND g.expectedInstallerOtherText IS NULL) OR g.expectedInstallerOtherText = :otherText)
              AND g.endAt >= :notEndingBefore
            ORDER BY g.endAt DESC
            """)
    List<PlanGroupJpaEntity> findLatestOfInstallerEndingAfter(@Param("clientId") String clientId,
            @Param("installer") InstallerName installer, @Param("otherText") String otherText,
            @Param("notEndingBefore") Instant notEndingBefore, Limit limit);

}
