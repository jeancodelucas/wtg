package com.projects.wtg.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account", schema = "appwtg")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;
    private String email;
    private String password;
    private String confirmPassword;
    private String token;
    private String secondEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    // Relacionamento 1-1 com User
    @JsonBackReference
    @ToString.Exclude
    @OneToOne(mappedBy = "account") // <-- lado inverso
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
}
