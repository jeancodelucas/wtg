package com.projects.wtg.dto;

import com.projects.wtg.model.Card;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CardDto {
    private Long id;
    private String cardName;
    private String docTitular;
    private String cardBand;
    private LocalDate expiresAt;

    public CardDto(Card card) {
        this.id = card.getId();
        this.cardName = card.getCardName();
        this.docTitular = card.getDocTitular();
        this.cardBand = card.getCardBand();
        this.expiresAt = card.getExpiresAt();
    }
}