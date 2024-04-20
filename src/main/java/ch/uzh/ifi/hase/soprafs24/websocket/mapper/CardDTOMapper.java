package ch.uzh.ifi.hase.soprafs24.websocket.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardGetDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardPutDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CardDTOMapper {

    CardDTOMapper INSTANCE = Mappers.getMapper(CardDTOMapper.class);

    // Mapping from Card Entity to CardGetDTO
    @Mapping(source = "code", target = "code")
    @Mapping(source = "image", target = "image")
    CardGetDTO convertEntityToCardGetDTO(Card card);

    // Mapping from CardPutDTO to Card Entity
    @Mapping(source = "code", target = "code")
    Card convertCardPutDTOToEntity(CardPutDTO cardPutDTO);
}
