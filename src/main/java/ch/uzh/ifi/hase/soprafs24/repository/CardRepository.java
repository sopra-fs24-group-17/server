package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("cardRepository")
public interface CardRepository extends JpaRepository<Card, Long> {

    Card findByCode(String code);

}
