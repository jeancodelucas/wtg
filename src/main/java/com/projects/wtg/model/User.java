package com.projects.wtg.model;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "user", schema = "appwtg") // mapeando a tabela existente
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

    // Relacionamento 1-1 com Account
    @JsonManagedReference
    @ToString.Exclude
    @OneToOne(cascade = CascadeType.ALL) // permite salvar a conta junto
    private Account account;

    // Relacionamento de um para muitos com promotion
    @JsonManagedReference
    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Promotion> promotions;
}
