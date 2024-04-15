package ch.uzh.ifi.hase.soprafs24.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name = "card")
public class Card implements Serializable {

    @Id
    private String code;

    @Column(nullable = false)
    private String value;

    @Column(nullable = false)
    private String suit;

    @Column(nullable = false)
    private String image;

    @Column
    private String deckId;

}
