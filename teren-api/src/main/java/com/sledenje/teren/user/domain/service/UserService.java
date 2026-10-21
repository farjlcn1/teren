package com.sledenje.teren.user.domain.service;

import com.sledenje.teren.user.domain.exception.UserEmailAlreadyExistsException;
import com.sledenje.teren.user.domain.exception.UserInUseException;
import com.sledenje.teren.user.domain.exception.UserNotFoundException;
import com.sledenje.teren.user.domain.exception.WeakPasswordException;
import com.sledenje.teren.user.domain.model.NewUser;
import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.model.UserPermissions;
import java.util.List;

public interface UserService {

    /**
     * @return vsi uporabniki v vrstnem redu vnosa
     */
    List<User> findAllUsers();

    /**
     * @throws UserNotFoundException če uporabnik s tem ID-jem ne obstaja
     */
    User findUserById(String userId);

    /**
     * Doda aktivnega uporabnika brez pravic.
     *
     * @throws WeakPasswordException           če geslo ne ustreza pravilom
     * @throws UserEmailAlreadyExistsException če email že uporablja drug uporabnik
     */
    User createUser(NewUser newUser);

    /**
     * Nastavi aktivnost in vse pravice hkrati.
     *
     * @throws UserNotFoundException če uporabnik s tem ID-jem ne obstaja
     */
    User updateAccess(String userId, boolean isActive, UserPermissions permissions);

    /**
     * Nastavi novo geslo in prekliče vse obstoječe seje uporabnika (glej {@link User#sessionsValidFrom()}).
     *
     * @throws UserNotFoundException če uporabnik s tem ID-jem ne obstaja
     * @throws WeakPasswordException če geslo ne ustreza pravilom
     */
    void changePassword(String userId, String password);

    /**
     * @throws UserNotFoundException če uporabnik s tem ID-jem ne obstaja
     * @throws UserInUseException    če ima uporabnik delovne naloge, urejanja ali dogodke v planu
     */
    void deleteUser(String userId);

}
