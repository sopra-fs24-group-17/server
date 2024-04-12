package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Notification;
import ch.uzh.ifi.hase.soprafs24.rest.dto.NotificationGetDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface NotificationDTOMapper {

    NotificationDTOMapper INSTANCE = Mappers.getMapper(NotificationDTOMapper.class);

    // Mapping for Game to GameGetDTO
    @Mapping(source = "message", target = "message")
    @Mapping(source = "timestamp", target = "timestamp")
    NotificationGetDTO convertEntityToNotificationGetDTO(Notification notification);

    // Conversion of GamePostDTO to Game Entity
    /*
    @Mapping(target = "mode", source = "mode")
    @Mapping(target = "maxPlayers", source = "maxPlayers")
    Notification convertNotificationPostDTOToEntity(NotificationPostDTO notificationPostDTO);
     */


}