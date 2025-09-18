package com.projects.wtg.model;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "`user`", schema = "appwtg")
@EntityListeners(AuditingEntityListener.class) // Habilita a auditoria
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String fullName;
    private LocalDate birthday;
    private Boolean active;
    private String phone;
    private String token;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private String pictureUrl;

    @JsonManagedReference
    @ToString.Exclude
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    @JsonManagedReference
    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Promotion> promotions;

    // Helper method para manter o relacionamento bidirecional consistente
    public void setAccount(Account account) {
        if (account == null) {
            if (this.account != null) {
                this.account.setUser(null);
            }
        } else {
            account.setUser(this);
        }
        this.account = account;
    }
}