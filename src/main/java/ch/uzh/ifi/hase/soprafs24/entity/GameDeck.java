package ch.uzh.ifi.hase.soprafs24.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "game_deck")
public class GameDeck implements Serializable {

    @Id
    private String deckID;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameId", referencedColumnName = "gameId")
    private Game game;

    @Column
    private String dealerPileId;

    @Column
    private Integer remainingCardsDealerStack;

    @Column
    private Integer remainingCardsDeck;

}
