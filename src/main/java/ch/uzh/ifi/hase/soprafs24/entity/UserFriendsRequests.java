package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "user_friendsrequests")
public class UserFriendsRequests implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User requestingUser;

    @ManyToOne
    @JoinColumn(name = "friend_id", referencedColumnName = "id")
    private User requestedUser;

    @Enumerated(EnumType.STRING)
    @Column
    private FriendRequestStatus status = FriendRequestStatus.PENDING;

    @Column(nullable = false)
    private Date requestdate;

    @PrePersist
    protected void onCreate() {requestdate = new Date();}
}
