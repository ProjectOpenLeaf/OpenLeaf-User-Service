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
        // ✅ Efficient: directly fetch from DB only users with therapist role
        List<UserEntity> entities = userRepository.findAllByRoles("client_therapist");

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
