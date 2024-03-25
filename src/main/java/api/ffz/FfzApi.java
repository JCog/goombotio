package api.ffz;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FfzApi {
    private static final String BASE_URI = "https://api.frankerfacez.com/v1/";
    
    private final RoomInterface proxy;
    
    public FfzApi() {
        ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();
        ResteasyWebTarget target = client.target(BASE_URI);
        proxy = target.proxy(RoomInterface.class);
    }
    
    public Map<String, String> getEmotes(String username) {
        Room roomById;
        try {
            roomById = proxy.getRoomById(username);
        } catch (ClientErrorException e) {
            System.out.printf("\nError getting FFZ emotes:\n%s\n", e.getMessage());
            return new HashMap<>();
        }
        List<Emoticon> emoticons = roomById.getSets().entrySet().iterator().next().getValue().getEmoticons();
        return emoticons.stream().collect(Collectors.toMap(e -> Integer.toString(e.getId()), Emoticon::getName));
    }
}
