package org.example.business.impl;

import lombok.RequiredArgsConstructor;
import org.example.business.UserRegister;
import org.example.domain.User;
import org.example.persistance.UserRepository;
import org.example.persistance.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserRegisterImpl implements UserRegister {

    private final UserRepository userRepository;

    @Override
    public User register(String keycloakId, String username, String email,
                         String firstName, String lastName) {

        if (userRepository.existsByKeycloakId(keycloakId)) {
            UserEntity existingUser = userRepository.findByKeycloakId(keycloakId).get();
            return toUser(existingUser);
        }

        UserEntity userEntity = UserEntity.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity savedEntity = userRepository.save(userEntity);

        return toUser(savedEntity);
    }

    private User toUser(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .keycloakId(entity.getKeycloakId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
