package org.example.business.impl;

import lombok.RequiredArgsConstructor;
import org.example.business.GetAllTherapists;
import org.example.domain.User;
import org.example.persistance.UserRepository;
import org.example.persistance.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAllTherapistsImpl implements GetAllTherapists {

    private final UserRepository userRepository;

    @Override
    public List<User> getAllTherapists() {
        // Note: This currently returns ALL users
        // Role filtering (therapist vs patient) happens in Keycloak
        // For now, frontend will need to manually identify therapists
        // Or you can add a 'role' column to UserEntity in the future

        List<UserEntity> entities = userRepository.findAll();

        return entities.stream()
                .map(this::toUser)
                .collect(Collectors.toList());
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
                .roles(entity.getRoles())
                .build();
    }
}
