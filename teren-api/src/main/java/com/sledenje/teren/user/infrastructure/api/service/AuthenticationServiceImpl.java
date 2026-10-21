package com.sledenje.teren.user.infrastructure.api.service;

import com.sledenje.teren.user.domain.exception.InvalidCredentialsException;
import com.sledenje.teren.user.domain.model.StoredCredentials;
import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.repository.UserRepository;
import com.sledenje.teren.user.domain.service.AuthenticationService;
import com.sledenje.teren.user.domain.service.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    // BCrypt upošteva največ 72 bajtov; daljših gesel ni mogel nastaviti noben obrazec.
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    /**
     * Hash brez uporabnika: ko email ne obstaja, se geslo vseeno preveri, da čas odziva ne razkrije obstoja računa.
     */
    private final String timingProtectionHash;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.timingProtectionHash = passwordHasher.hash("ni-uporabnika-" + System.nanoTime());
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(String email, String password) {
        Optional<StoredCredentials> credentials = userRepository.findCredentialsByEmail(User.normalizeEmail(email));
        String passwordHash = credentials.map(StoredCredentials::passwordHash).orElse(timingProtectionHash);
        boolean passwordMatches = password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES
                && passwordHasher.matches(password, passwordHash);

        if (credentials.isEmpty() || !passwordMatches || !credentials.get().user().isActive()) {
            throw new InvalidCredentialsException();
        }
        User user = credentials.get().user();
        log.info("Prijava uporabnika {}", user.id());
        return user;
    }

}
