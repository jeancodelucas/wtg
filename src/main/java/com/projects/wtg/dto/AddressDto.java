package com.projects.wtg.dto;

import com.projects.wtg.model.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AddressDto {
    @NotBlank(message = "O campo 'address' não pode estar em branco")
    private String address;

    @NotNull(message = "O campo 'number' é obrigatório")
    private Integer number;

    @NotBlank(message = "O campo 'complement' не pode estar em branco")
    private String complement;

    @NotBlank(message = "O campo 'reference' não pode estar em branco")
    private String reference;

    @NotBlank(message = "O campo 'postalCode' não pode estar em branco")
    private String postalCode;

    private String obs;

    public AddressDto(Address address) {
        if (address != null) {
            this.address = address.getAddress();
            this.number = address.getNumber();
            this.complement = address.getComplement();
            this.obs = address.getObs();
            this.reference = address.getReference();
            this.postalCode = address.getPostalCode();
        }
    }
}