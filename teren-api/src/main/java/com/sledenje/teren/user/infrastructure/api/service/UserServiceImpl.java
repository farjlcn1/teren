package com.sledenje.teren.user.infrastructure.api.service;

import com.sledenje.teren.user.domain.exception.UserEmailAlreadyExistsException;
import com.sledenje.teren.user.domain.exception.UserInUseException;
import com.sledenje.teren.user.domain.exception.UserNotFoundException;
import com.sledenje.teren.user.domain.exception.WeakPasswordException;
import com.sledenje.teren.user.domain.model.NewUser;
import com.sledenje.teren.user.domain.model.PasswordPolicy;
import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.model.UserPermissions;
import com.sledenje.teren.user.domain.repository.UserRepository;
import com.sledenje.teren.user.domain.service.PasswordHasher;
import com.sledenje.teren.user.domain.service.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAllUsersOrderedByCreation();
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserById(String userId) {
        return userRepository.findUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    @Transactional
    public User createUser(NewUser newUser) {
        requireStrongPassword(newUser.password());
        String email = User.normalizeEmail(newUser.email());
        if (userRepository.existsUserByEmail(email)) {
            throw new UserEmailAlreadyExistsException();
        }
        User createdUser = userRepository.saveNewUser(email, newUser.fullName().trim(),
                passwordHasher.hash(newUser.password()));
        log.info("Dodan uporabnik {}", createdUser.id());
        return createdUser;
    }

    @Override
    @Transactional
    public User updateAccess(String userId, boolean isActive, UserPermissions permissions) {
        User updatedUser = userRepository.updateAccess(userId, isActive, permissions)
                .orElseThrow(() -> new UserNotFoundException(userId));
        log.info("Posodobljene pravice uporabnika {} (aktiven: {})", userId, isActive);
        return updatedUser;
    }

    @Override
    @Transactional
    public void changePassword(String userId, String password) {
        requireStrongPassword(password);
        if (!userRepository.updatePasswordHash(userId, passwordHasher.hash(password))) {
            throw new UserNotFoundException(userId);
        }
        log.info("Spremenjeno geslo uporabnika {}", userId);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        if (!userRepository.existsUserById(userId)) {
            throw new UserNotFoundException(userId);
        }
        if (userRepository.isUserReferenced(userId)) {
            throw new UserInUseException();
        }
        userRepository.deleteUserById(userId);
        log.info("Izbrisan uporabnik {}", userId);
    }

    private static void requireStrongPassword(String password) {
        PasswordPolicy.findViolation(password).ifPresent(violation -> {
            throw new WeakPasswordException(violation);
        });
    }

}
