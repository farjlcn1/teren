package com.sledenje.teren.workorder.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkOrderJpaRepository extends JpaRepository<WorkOrderJpaEntity, String> {

    @Query("SELECT DISTINCT w.imei FROM WorkOrderJpaEntity w ORDER BY w.imei")
    List<String> findDistinctImeis();

}
