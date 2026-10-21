package com.sledenje.teren.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.sledenje.teren.TestcontainersConfiguration;
import com.sledenje.teren.user.domain.model.PasswordPolicy;
import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.service.UserService;
import com.sledenje.teren.user.infrastructure.config.FirstAdminInitializer;
import com.sledenje.teren.user.infrastructure.config.FirstAdminProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FirstAdminInitializerTest {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUsers() {
        jdbcTemplate.execute("TRUNCATE TABLE \"users\" CASCADE");
    }

    private void runWithEmail(String email) {
        new FirstAdminInitializer(userService, new FirstAdminProperties(email)).run(new DefaultApplicationArguments());
    }

    @Test
    void createsAdminWithAllPermissionsOnEmptyDatabase() {
        runWithEmail("Prvi.Admin@Example.com");

        List<User> users = userService.findAllUsers();
        assertThat(users).hasSize(1);
        User admin = users.getFirst();
        assertThat(admin.email()).isEqualTo("prvi.admin@example.com");
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.permissions().canManageUsers()).isTrue();
        assertThat(admin.permissions().canManagePlan()).isTrue();
    }

    @Test
    void doesNothingWhenUsersExistOrEmailIsMissing() {
        runWithEmail("");
        assertThat(userService.findAllUsers()).isEmpty();

        runWithEmail("prvi@example.com");
        runWithEmail("drugi@example.com");
        assertThat(userService.findAllUsers()).extracting(User::email).containsExactly("prvi@example.com");
    }

    @Test
    void generatedPasswordSatisfiesPolicy() {
        FirstAdminInitializer initializer = new FirstAdminInitializer(userService, new FirstAdminProperties(null));
        for (int i = 0; i < 50; i++) {
            assertThat(PasswordPolicy.findViolation(initializer.generatePassword())).isEmpty();
        }
    }

}
