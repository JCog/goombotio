package dev.jcog.goombotio.api.seventv;

import dev.jcog.goombotio.api.seventv.user.Emote;
import dev.jcog.goombotio.api.seventv.user.User;
import dev.jcog.goombotio.api.seventv.user.UserInterface;
import jakarta.ws.rs.ClientErrorException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SevenTvApi {
    private static final Logger log = LoggerFactory.getLogger(SevenTvApi.class);
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
            log.error("Error getting 7TV emotes: {}", e.getMessage());
            return new HashMap<>();
        }
        List<Emote> emoticons = user.getEmoteSet().getEmotes();
        return emoticons.stream().collect(Collectors.toMap(Emote::getId, Emote::getName));
    }
}
