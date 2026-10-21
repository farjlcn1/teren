package com.sledenje.teren.common.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Persistable;

/**
 * Skupna osnova za tabele, ki jih je ustvarila Prisma.
 * <p>
 * Prisma generira ID-je in {@code updatedAt} v aplikaciji (baza zanje nima privzetih vrednosti),
 * zato jih mora nastaviti tudi API. Časi se hranijo kot {@code TIMESTAMP(3)} v UTC, zato jih
 * odrežemo na milisekunde. Ker je ID dodeljen ročno, {@link Persistable#isNew()} pove Spring Data,
 * naj nov zapis vstavi brez predhodnega SELECT-a.
 */
@MappedSuperclass
public abstract class TimestampedJpaEntity implements Persistable<String> {

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
