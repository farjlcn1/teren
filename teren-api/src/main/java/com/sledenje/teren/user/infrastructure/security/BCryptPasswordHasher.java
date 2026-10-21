package com.sledenje.teren.user.infrastructure.security;

import com.sledenje.teren.user.domain.service.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    // Enako kot BCRYPT_ROUNDS v aplikaciji Teren, da imajo vsi hashi enako ceno.
    private static final int STRENGTH = 12;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(STRENGTH);

    @Override
    public String hash(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean matches(String password, String passwordHash) {
        return passwordEncoder.matches(password, passwordHash);
    }

}
