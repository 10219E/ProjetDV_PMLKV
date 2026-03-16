package lu.ephec.backend_projetdv2026.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", length = 20)
    private String matricule;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "fname", length = 50, nullable = false)
    private String firstName;

    @Column(name = "lname", length = 50, nullable = false)
    private String lastName;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "bdate")
    private LocalDate birthDate;

    @Column(name = "role_id")
    private Short roleId;

    @Column(name = "lvl", length = 20, nullable = false)
    private String level;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created")
    private LocalDateTime created;

    @Column(name = "auth", length = 255)
    private String auth;
}
