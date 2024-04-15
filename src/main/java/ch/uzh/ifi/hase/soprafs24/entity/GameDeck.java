package ch.uzh.ifi.hase.soprafs24.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name = "game_deck")
public class GameDeck implements Serializable {

    @Id
    private String deckID;

    @Column
    private Long remainingCards;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameId", referencedColumnName = "gameId")
    private Game game;
}
