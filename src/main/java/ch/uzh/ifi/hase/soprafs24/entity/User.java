package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.TutorialFlag;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.constant.ProfileVisibility;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "users")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false, unique = true)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private Boolean otp = false;

  @Column(nullable = false)
  private Date creationdate;

  @Column
  private Date birthdate;

  @Column(nullable = false, unique = true)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column
  private ProfileVisibility profilevisibility = ProfileVisibility.FALSE;;

  @Column
  private String countryoforigin;

  @Column
  private String avatar;

  @Enumerated(EnumType.STRING)
  @Column
  private TutorialFlag tutorialflag = TutorialFlag.TRUE;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private UserStats userStats;

  @PrePersist
  protected void onCreate() {
      creationdate = new Date();
      createUserStats();
  }

  private void createUserStats(){
      UserStats userStats = new UserStats();
      userStats.setUser(this);
      this.setUserStats(userStats);
  }
}