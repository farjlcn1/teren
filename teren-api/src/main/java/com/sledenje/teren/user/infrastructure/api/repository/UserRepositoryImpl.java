package com.sledenje.teren.user.infrastructure.api.repository;

import com.sledenje.teren.user.domain.model.StoredCredentials;
import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.model.UserPermissions;
import com.sledenje.teren.user.domain.repository.UserRepository;
import com.sledenje.teren.user.infrastructure.persistence.UserJpaEntity;
import com.sledenje.teren.user.infrastructure.persistence.UserJpaRepository;
import com.sledenje.teren.user.infrastructure.util.UserJpaConverter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public List<User> findAllUsersOrderedByCreation() {
        return userJpaRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(UserJpaConverter::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findUserById(String userId) {
        return userJpaRepository.findById(userId).map(UserJpaConverter::toDomain);
    }

    @Override
    public boolean existsUserByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<StoredCredentials> findCredentialsByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(UserJpaConverter::toCredentials);
    }

    @Override
    public boolean existsUserById(String userId) {
        return userJpaRepository.existsById(userId);
    }

    @Override
    public boolean isUserReferenced(String userId) {
        return userJpaRepository.isReferencedByWorkOrdersEditsOrPlanGroups(userId);
    }

    @Override
    public User saveNewUser(String email, String fullName, String passwordHash) {
        // Obstoječi zapisi imajo cuid ID-je (Prisma), novi dobijo UUID; stolpec je TEXT in format ni preverjan.
        UserJpaEntity savedEntity = userJpaRepository.save(
                new UserJpaEntity(UUID.randomUUID().toString(), email, fullName, passwordHash));
        return UserJpaConverter.toDomain(savedEntity);
    }

    @Override
    public Optional<User> updateAccess(String userId, boolean isActive, UserPermissions permissions) {
        return userJpaRepository.findById(userId).map(entity -> {
            UserJpaConverter.applyAccess(entity, isActive, permissions);
            // Flush sproži @PreUpdate, da vrnjeni uporabnik že vsebuje nov updatedAt.
            return UserJpaConverter.toDomain(userJpaRepository.saveAndFlush(entity));
        });
    }

    @Override
    public boolean updatePasswordHash(String userId, String passwordHash) {
        return userJpaRepository.findById(userId)
                .map(entity -> {
                    entity.changePasswordHash(passwordHash);
                    userJpaRepository.save(entity);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public void deleteUserById(String userId) {
        userJpaRepository.deleteById(userId);
    }

}
