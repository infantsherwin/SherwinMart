package com.sherwin.sherwinmart.dto;

import com.sherwin.sherwinmart.model.User;

/**
 * Client-facing representation of a user. Deliberately excludes passwordHash
 * (Section 13, rule 4: DTOs are separate from entities and never leak secrets).
 */
public class UserResponseDTO {

    private final Long id;
    private final String name;
    private final String email;
    private final String role;

    private UserResponseDTO(Long id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    /** Builder — required design pattern (Section 12) for constructing this DTO. */
    public static class Builder {
        private Long id;
        private String name;
        private String email;
        private String role;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public UserResponseDTO build() {
            return new UserResponseDTO(id, name, email, role);
        }
    }

    public static UserResponseDTO fromEntity(User user) {
        return new Builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
