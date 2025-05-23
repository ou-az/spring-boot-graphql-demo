package com.example.userservice.config;

import com.example.userservice.model.ERole;
import com.example.userservice.model.Role;
import com.example.userservice.model.User;
import com.example.userservice.repository.RoleRepository;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (roleRepository.count() > 0) {
                log.info("Roles already initialized, skipping initialization");
                return;
            }

            log.info("Initializing roles data");

            // Create roles
            Role userRole = new Role(null, ERole.ROLE_USER);
            Role modRole = new Role(null, ERole.ROLE_MODERATOR);
            Role adminRole = new Role(null, ERole.ROLE_ADMIN);
            
            // Save all roles first
            roleRepository.save(userRole);
            roleRepository.save(modRole);
            roleRepository.save(adminRole);

            log.info("Created roles: USER, MODERATOR, ADMIN");

            // Create sample users
            if (userRepository.count() == 0) {
                log.info("Creating sample users");

                // Regular user
                Set<Role> userRoles = new HashSet<>();
                // Use findByName to get a fresh instance from the persistence context
                roleRepository.findByName(ERole.ROLE_USER).ifPresent(userRoles::add);
                
                User regularUser = User.builder()
                        .username("user")
                        .email("user@example.com")
                        .password(passwordEncoder.encode("password"))
                        .firstName("Regular")
                        .lastName("User")
                        .roles(userRoles)
                        .enabled(true)
                        .build();
                
                userRepository.save(regularUser);

                // Admin user
                Set<Role> adminRoles = new HashSet<>();
                roleRepository.findByName(ERole.ROLE_USER).ifPresent(adminRoles::add);
                roleRepository.findByName(ERole.ROLE_MODERATOR).ifPresent(adminRoles::add);
                roleRepository.findByName(ERole.ROLE_ADMIN).ifPresent(adminRoles::add);
                
                User adminUser = User.builder()
                        .username("admin")
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("admin"))
                        .firstName("Admin")
                        .lastName("User")
                        .roles(adminRoles)
                        .enabled(true)
                        .build();
                
                userRepository.save(adminUser);

                log.info("Created sample users: 'user' and 'admin'");
            }
        };
    }
}
