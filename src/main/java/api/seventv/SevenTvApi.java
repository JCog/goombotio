package api.seventv;

import api.seventv.user.Emote;
import api.seventv.user.User;
import api.seventv.user.UserInterface;
import jakarta.ws.rs.ClientErrorException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SevenTvApi {
    private static final String BASE_URI = "https://7tv.io/v3/";
    
    private final UserInterface proxy;
    
    public SevenTvApi(ResteasyClient client) {
        ResteasyWebTarget target = client.target(BASE_URI);
        proxy = target.proxy(UserInterface.class);
    }
    
    public Map<String, String> getEmotes(String userId) {
        User user;
        try {
            user = proxy.getUserById(userId);
        } catch (ClientErrorException e) {
            System.out.printf("\nError getting 7TV emotes:\n%s\n", e.getMessage());
            return new HashMap<>();
        }
        List<Emote> emoticons = user.getEmoteSet().getEmotes();
        return emoticons.stream().collect(Collectors.toMap(Emote::getId, Emote::getName));
    }
}
