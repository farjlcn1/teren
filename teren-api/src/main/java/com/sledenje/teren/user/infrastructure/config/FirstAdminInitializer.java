package com.sledenje.teren.user.infrastructure.config;

import com.sledenje.teren.user.domain.model.NewUser;
import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.model.UserPermissions;
import com.sledenje.teren.user.domain.service.UserService;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Ob prvem zagonu na prazni bazi ustvari admina z vsemi pravicami in naključnim geslom, ki ga izpiše v log
 * (nadomešča nekdanji {@code teren-seed}). Če uporabniki že obstajajo, ne naredi ničesar.
 */
@Component
public class FirstAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FirstAdminInitializer.class);

    private static final UserPermissions ALL_PERMISSIONS = new UserPermissions(true, true, true, true, true, true, true, true);

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final int PASSWORD_LENGTH = 16;

    private final SecureRandom random = new SecureRandom();
    private final UserService userService;
    private final FirstAdminProperties firstAdminProperties;

    public FirstAdminInitializer(UserService userService, FirstAdminProperties firstAdminProperties) {
        this.userService = userService;
        this.firstAdminProperties = firstAdminProperties;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        String email = firstAdminProperties.email();
        if (email == null || email.isBlank() || !userService.findAllUsers().isEmpty()) {
            return;
        }
        String password = generatePassword();
        User admin = userService.createUser(new NewUser(email, "Admin", password));
        userService.updateAccess(admin.id(), true, ALL_PERMISSIONS);
        log.warn("Ustvarjen prvi admin {} z geslom: {}  (geslo zamenjaj po prvi prijavi v Admin → Uporabniki)",
                admin.email(), password);
    }

    /**
     * Vsaj ena velika in mala črka, številka in poseben znak (pravila gesla), brez podobnih znakov (0/O, 1/l).
     */
    public String generatePassword() {
        List<Character> characters = new ArrayList<>();
        characters.add(randomFrom(UPPER));
        characters.add(randomFrom(LOWER));
        characters.add(randomFrom(DIGITS));
        characters.add(randomFrom(SPECIAL));
        String all = UPPER + LOWER + DIGITS + SPECIAL;
        while (characters.size() < PASSWORD_LENGTH) {
            characters.add(randomFrom(all));
        }
        Collections.shuffle(characters, random);
        StringBuilder password = new StringBuilder();
        characters.forEach(password::append);
        return password.toString();
    }

    private char randomFrom(String characters) {
        return characters.charAt(random.nextInt(characters.length()));
    }

}
