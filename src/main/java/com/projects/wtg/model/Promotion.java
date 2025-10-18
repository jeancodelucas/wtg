package com.projects.wtg.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.ArrayList; // Import necessário
import java.util.List;
import java.util.ArrayList; //

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name= "promotion", schema = "appwtg")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "promotion_id_seq")
    @SequenceGenerator(name = "promotion_id_seq", sequenceName = "promotion_id_seq", schema = "appwtg", allocationSize = 1)
    private Long id;

    private String title;
    private String description;
    private boolean completeRegistration;
    private String obs;
    private boolean highlight;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "geography(Point, 4326)")
    private Point point;

    @Column(name = "allow_user_active_promotion")
    private Boolean allowUserActivePromotion;

    @Column(name = "promotion_type")
    private PromotionType promotionType;

    private Boolean active;

    // --- CORREÇÃO: ADICIONANDO O CAMPO QUE FALTAVA ---
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    @JsonManagedReference
    private Address address; // Este é o campo que o mappedBy="address" estava procurando

    @JsonBackReference
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("promotion-comments")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("promotion-images")
    @Builder.Default
    private List<PromotionImage> images = new ArrayList<>();
}