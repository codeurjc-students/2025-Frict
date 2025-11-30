package com.tfg.backend.model;

import com.tfg.backend.utils.AttributeEncryptor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;

@Entity
@Getter
@Setter
@Table(name = "payment_cards")
public class PaymentCard {
    //Does not reference User entity as it is an exclusive entity of User (and it will always be accessed from User)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardOwnerName;

    @Convert(converter = AttributeEncryptor.class)
    private String number;

    @Convert(converter = AttributeEncryptor.class)
    private String cvv;

    private YearMonth dueDate;

    public PaymentCard() {
    }

    public PaymentCard(String cardOwnerName, String number, String cvv, YearMonth dueDate) {
        this.cardOwnerName = cardOwnerName;
        this.number = number;
        this.cvv = cvv;
        this.dueDate = dueDate;
    }
}
