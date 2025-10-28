package org.example.business;

import org.example.domain.User;

import java.util.Set;

public interface UserRegister {
    User register(String keycloakId, String username, String email,
                  String firstName, String lastName, Set<String> roles);
}
