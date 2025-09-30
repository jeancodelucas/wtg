package com.projects.wtg.dto;

import com.projects.wtg.model.Wallet;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class WalletDto {
    private Long id;
    private List<CardDto> cards;

    public WalletDto(Wallet wallet) {
        this.id = wallet.getId();
        if (wallet.getCards() != null) {
            this.cards = wallet.getCards().stream()
                    .map(CardDto::new)
                    .collect(Collectors.toList());
        }
    }
}