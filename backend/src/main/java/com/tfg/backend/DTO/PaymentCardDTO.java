package com.tfg.backend.DTO;

import com.tfg.backend.model.PaymentCard;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

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

    //Visualization and edit purposes (for safety reasons, number and cvv will not be editable)
    public PaymentCardDTO(PaymentCard card) {
        this.id = card.getId();
        this.alias = card.getAlias();
        this.cardOwnerName = card.getCardOwnerName();
        this.numberEnding = card.getNumber().substring(card.getNumber().length() - 4); //4 last digits of the card number
        this.number = "**** **** **** " + this.numberEnding;
        this.cvv = "***";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        this.dueDate = card.getDueDate().format(formatter);
    }
}