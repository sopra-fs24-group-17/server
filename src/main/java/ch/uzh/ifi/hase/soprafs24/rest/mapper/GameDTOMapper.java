package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper
public interface GameDTOMapper {

    GameDTOMapper INSTANCE = Mappers.getMapper(GameDTOMapper.class);

    // Mapping for Game to GameGetDTO
    @Mapping(source = "gameId", target = "gameId")
    @Mapping(source = "mode", target = "mode")
    @Mapping(source = "maxPlayers", target = "maxPlayers")
    @Mapping(source = "initiatingUser.username", target = "initiatingUserName")
    @Mapping(source = "state", target = "state")
    @Mapping(target = "deckId", ignore = true)
    GameGetDTO convertEntityToGameGetDTO(Game game);

    // Conversion of GamePostDTO to Game Entity
    @Mapping(target = "mode", source = "mode")
    @Mapping(target = "maxPlayers", source = "maxPlayers")
    Game convertGamePostDTOToEntity(GamePostDTO gamePostDTO);
}
