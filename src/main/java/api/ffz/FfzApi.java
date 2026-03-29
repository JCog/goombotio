package api.ffz;

import api.ffz.room.Emoticon;
import api.ffz.room.Room;
import api.ffz.room.RoomInterface;
import jakarta.ws.rs.ClientErrorException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FfzApi {
    private static final Logger log = LoggerFactory.getLogger(FfzApi.class);
    private static final String BASE_URI = "https://api.frankerfacez.com/v1/";

    private final RoomInterface proxy;
    
    public FfzApi(ResteasyClient client) {
        ResteasyWebTarget target = client.target(BASE_URI);
        proxy = target.proxy(RoomInterface.class);
    }
    
    public Map<String, String> getEmotes(String username) {
        Room roomById;
        try {
            roomById = proxy.getRoomById(username);
        } catch (ClientErrorException e) {
            log.error("Error getting FFZ emotes: {}", e.getMessage());
            return new HashMap<>();
        }
        List<Emoticon> emoticons = roomById.getSets().entrySet().iterator().next().getValue().getEmoticons();
        return emoticons.stream().collect(Collectors.toMap(e -> Integer.toString(e.getId()), Emoticon::getName));
    }
}
