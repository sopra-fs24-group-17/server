package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "ruleset")
public class RuleSet implements Serializable {

    @Id
    private Long gameId;

    @Column(nullable = false)
    private Integer timeLimit;

    @Column(nullable = false)
    private Integer maxPlayers;

    @Column(nullable = false)
    private String winCondition; // Assuming textual description; adjust as required later

    @Column(nullable = false)
    private Double explosionChance;

    @OneToOne
    @MapsId
    @JoinColumn(name = "gameId")
    private Game game;

}