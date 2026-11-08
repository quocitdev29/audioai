package com.example.audioai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal balance;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "hovaten", length = 255)
    private String hovaten;
}
