package com.sledenje.teren.user.domain.repository;

import com.sledenje.teren.user.domain.model.StoredCredentials;
import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.model.UserPermissions;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    List<User> findAllUsersOrderedByCreation();

    Optional<User> findUserById(String userId);

    boolean existsUserByEmail(String email);

    Optional<StoredCredentials> findCredentialsByEmail(String email);

    boolean existsUserById(String userId);

    /**
     * @return {@code true}, če je uporabnik ustvaril ali urejal delovni nalog ali ustvaril dogodek v planu
     */
    boolean isUserReferenced(String userId);

    User saveNewUser(String email, String fullName, String passwordHash);

    /**
     * @return posodobljen uporabnik ali prazno, če uporabnik s tem ID-jem ne obstaja
     */
    Optional<User> updateAccess(String userId, boolean isActive, UserPermissions permissions);

    /**
     * Zamenja geslo in prekliče seje, izdane pred {@code changedAt}.
     *
     * @return {@code false}, če uporabnik s tem ID-jem ne obstaja
     */
    boolean updatePassword(String userId, String passwordHash, Instant changedAt);

    void deleteUserById(String userId);

}
