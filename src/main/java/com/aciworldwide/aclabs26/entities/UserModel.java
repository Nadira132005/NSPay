package com.aciworldwide.aclabs26.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.aciworldwide.aclabs26.enums.UserRole;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "USERS")
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String name;
    private String firstName;
    private String lastName;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private Long accountId;
    private boolean passwordChangeRequired;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.User;

    @PrePersist //just before insertion
    public void applyDefaultRole() {
        if (role == null) {
            role = UserRole.User;
        }
    }
}
