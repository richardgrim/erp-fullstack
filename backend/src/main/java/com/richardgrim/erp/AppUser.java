package com.richardgrim.erp;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, unique = true, length = 40) String username;
    @Column(nullable = false, length = 80) String displayName;
    @Column(nullable = false, length = 100) String passwordHash;
    @Column(nullable = false, length = 20) String role;
    @Column(nullable = false) Instant createdAt;
    protected AppUser() {}
    AppUser(String username, String displayName, String passwordHash, String role) {
        this.username = username;
        this.displayName = displayName.strip();
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = Instant.now();
    }
}
