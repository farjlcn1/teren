package com.sledenje.teren.client.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, String> {

    List<ClientJpaEntity> findAllByOrderByNameAsc();

}
