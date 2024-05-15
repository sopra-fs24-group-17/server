package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.GameState;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "game")
public class Game implements Serializable {

    @Id
    private Long gameId;

    @ManyToOne
    @JoinColumn(name = "initiatingId", referencedColumnName = "id")
    private User initiatingUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameMode mode;

    @Column(nullable = false)
    private Integer maxPlayers;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "game_stats_players",
            joinColumns = @JoinColumn(name = "gameId"),
            inverseJoinColumns = @JoinColumn(name = "id")
    )
    private List<User> players = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "currentTurn", referencedColumnName = "id")
    private User currentTurn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameState state = GameState.PREPARING;

    @ManyToOne
    @JoinColumn(name = "winningPlayerId", referencedColumnName = "id")
    private User winningPlayer;

    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private GameDeck gameDeck;

    @Column(nullable = false)
    private Date creationdate = new Date();

    private boolean skipDraw = false;

    private  boolean attacked = false;

    private boolean repeatTurn = false;
}