package com.projects.wtg.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "address", schema = "appwtg")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String address;
    private Integer number;
    private String complement;
    private String obs;
    private String reference;

    @Column(name = "postal_code")
    private String postalCode;

    // Lado inverso da relação, mapeado pelo campo "address" na entidade Promotion
    @OneToOne(mappedBy = "address")
    @JsonBackReference
    private Promotion promotion;
}