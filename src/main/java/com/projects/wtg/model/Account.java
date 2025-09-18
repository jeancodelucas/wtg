package com.projects.wtg.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
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
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private Boolean emailVerified;
    private String loginProvider;
    private String locale;
    private String loginSub;

    // Relacionamento 1-1 com User (Lado inverso)
    @JsonBackReference
    @ToString.Exclude
    @OneToOne(mappedBy = "account")
    private User user;

}