package api.bttv;

import api.bttv.user.Emote;
import api.bttv.user.User;
import api.bttv.user.UserInterface;
import jakarta.ws.rs.ClientErrorException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BttvApi {
    private static final String BASE_URI = "https://api.betterttv.net/3/";
    
    private final UserInterface proxy;
    
    public BttvApi(ResteasyClient client) {
        ResteasyWebTarget target = client.target(BASE_URI);
        proxy = target.proxy(UserInterface.class);
    }
    
    public Map<String, String> getEmotes(String userId) {
        User user;
        try {
            user = proxy.getUserById(userId);
        } catch (ClientErrorException e) {
            System.out.printf("\nError getting BTTV emotes:\n%s\n", e.getMessage());
            return new HashMap<>();
        }
        List<Emote> emoticons = user.getEmotes();
        return emoticons.stream().collect(Collectors.toMap(Emote::getId, Emote::getCode));
    }
}
