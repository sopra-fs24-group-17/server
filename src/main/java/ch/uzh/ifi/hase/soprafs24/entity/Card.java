package ch.uzh.ifi.hase.soprafs24.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name = "card")
public class Card implements Serializable {

    @Id
    private String code;

    @Column(nullable = false)
    private String internalCode;

    @Column(nullable = false)
    private String suit;

    @Column(nullable = false)
    private String image;

    @Column
    private String deckId;

    public Card() {
    }

    @PrePersist
    @PreUpdate
    private void beforeSave() {
        mapCodeToInternalRepresentation();
    }

    private void mapCodeToInternalRepresentation() {
        if (code != null) {
            switch (code) {
                case "KS":
                case "KH":
                case "KC":
                case "KD":
                case "X1":
                case "X2":
                    internalCode = "defuse";
                    break;
                case "AS":
                case "AH":
                case "AC":
                case "AD":
                    internalCode = "explosion";
                    break;
                case "JS":
                case "JD":
                case "JC":
                case "JH":
                    internalCode = "attack";
                    break;
                case "0S":
                case "0D":
                case "0C":
                case "0H":
                    internalCode = "skip";
                    break;
                case "9S":
                case "9D":
                case "9C":
                case "9H":
                    internalCode = "nope";
                    break;
                case "8S":
                case "8D":
                case "8C":
                case "8H":
                    internalCode = "favor";
                    break;
                case "7S":
                case "7D":
                case "7C":
                case "7H":
                case "QC":
                case "QH":
                    internalCode = "shuffle";
                    break;
                case "6S":
                case "6D":
                case "6C":
                case "6H":
                case "QS":
                case "QD":
                    internalCode = "future";
                    break;
                case "5S":
                case "5D":
                case "5C":
                case "5H":
                    internalCode = "tacocat";
                    break;
                case "4S":
                case "4D":
                case "4C":
                case "4H":
                    internalCode = "cattermelon";
                    break;
                case "3S":
                case "3D":
                case "3C":
                case "3H":
                    internalCode = "hairypotatocat";
                    break;
                case "2S":
                case "2D":
                case "2C":
                case "2H":
                    internalCode = "beardcat";
                    break;
                default:
                    internalCode = "unknown";
                    break;
            }
        }
    }
}