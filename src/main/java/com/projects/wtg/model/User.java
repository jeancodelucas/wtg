package com.projects.wtg.model;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "`user`", schema = "appwtg")
@EntityListeners(AuditingEntityListener.class)
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
    // private Boolean active; // REMOVIDO DESTA CLASSE
    private String phone;
    private String token;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "user_type")
    private UserType userType;

    private String pictureUrl;

    @JsonManagedReference
    @ToString.Exclude
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    @JsonManagedReference
    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Promotion> promotions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserPlan> userPlans = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @ToString.Exclude
    private Wallet wallet;

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

    public void addPromotion(Promotion promotion) {
        this.promotions.add(promotion);
        promotion.setUser(this);
    }

    public void setWallet(Wallet wallet) {
        if (wallet == null) {
            if (this.wallet != null) {
                this.wallet.setUser(null);
            }
        } else {
            wallet.setUser(this);
        }
        this.wallet = wallet;
    }
}