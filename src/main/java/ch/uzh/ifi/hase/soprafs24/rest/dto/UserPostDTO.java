package ch.uzh.ifi.hase.soprafs24.rest.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserPostDTO {

  private String username;
  private String email;
  private String password;

}
