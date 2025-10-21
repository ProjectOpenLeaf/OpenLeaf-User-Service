package org.example.business;

import org.example.domain.User;

public interface UserRegister {
    User register(String keycloakId, String username, String email,
                  String firstName, String lastName);
}
