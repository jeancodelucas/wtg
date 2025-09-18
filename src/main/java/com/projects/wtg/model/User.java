package com.projects.wtg.model;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user", schema = "appwtg")
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String pictureUrl;

    // Relacionamento 1-1 com Account (Lado dono)
    @JsonManagedReference
    @ToString.Exclude
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    // Relacionamento de um para muitos com promotion
    @JsonManagedReference
    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Promotion> promotions;


    public void setAccount(Account account) { //cria a relação de user-account 1-1 sem precisar ficar setando o user no account.
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