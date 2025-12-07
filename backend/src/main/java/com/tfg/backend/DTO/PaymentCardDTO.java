package com.tfg.backend.DTO;

import com.tfg.backend.model.PaymentCard;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCardDTO {
    private Long id;
    private String alias;
    private String cardOwnerName;
    private String number;
    private String numberEnding; //4 last characters of variable number, to be shown in screens
    private String cvv;
    private String dueDate;

    public PaymentCardDTO() {
    }

    public PaymentCardDTO(String alias, String cardOwnerName, String number, String cvv, String dueDate) {
        this.alias = alias;
        this.cardOwnerName = cardOwnerName;
        this.number = number;
        this.numberEnding = number.substring(number.length() - 4); //4 last digits of the card number
        this.cvv = cvv;
        this.dueDate = dueDate;
    }

    //Visualization purposes only
    public PaymentCardDTO(PaymentCard card) {
        this.id = card.getId();
        this.alias = card.getAlias();
        this.cardOwnerName = card.getCardOwnerName();
        this.numberEnding = card.getNumber().substring(card.getNumber().length() - 4); //4 last digits of the card number
        this.dueDate = card.getDueDate().toString();
    }
}