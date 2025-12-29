package com.anasol.cafe.service;




import com.anasol.cafe.entity.Role;
import com.anasol.cafe.entity.User;
import com.anasol.cafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!userRepository.existsByRole(Role.ADMIN)) {

            User admin = new User();
            admin.setEmail("admin@cafe.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            admin.setFirstLogin(true);
            admin.setActive(true);

            userRepository.save(admin);
        }
    }
}


