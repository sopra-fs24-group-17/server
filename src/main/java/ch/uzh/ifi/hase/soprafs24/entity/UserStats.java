package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "user_stats")
public class UserStats implements Serializable {

    @OneToOne
    @MapsId
    @JoinColumn(name = "userId")
    private User user;

    @Id
    private Long userId;

    @Column(nullable = false)
    private Integer gamesPlayed = 0;

    @Column(nullable = false)
    private Integer gamesWon = 0;

    @Column(nullable = false)
    private Double winLossRatio = 0.0;

    @Column(nullable = false)
    private Integer totalFriends = 0;

    @Column(nullable = false)
    private Integer achievementsUnlocked = 0;

    @Column()
    private Date lastPlayed;

    @PrePersist
    @PreUpdate
    private void calculateWinLossRatio() {
        if (gamesPlayed != null && gamesPlayed > 0) {
            this.winLossRatio = (double) gamesWon / (gamesPlayed + gamesWon);
        } else {
            this.winLossRatio = 0.0;
        }
    }
}