package api.racetime;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.util.ArrayList;
import java.util.List;

public class RacetimeApi {
    private static final String BASE_URI = "https://racetime.gg";
    
    private final GameDataInterface gameDataProxy;
    private final RaceDataInterface raceDataProxy;
    
    public RacetimeApi() {
        ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();
        ResteasyWebTarget target = client.target(BASE_URI);
        gameDataProxy = target.proxy(GameDataInterface.class);
        raceDataProxy = target.proxy(RaceDataInterface.class);
    }
    
    public List<Race> getCurrentRaces(String gameSlug) {
        GameData currentRaces;
        try {
            currentRaces = gameDataProxy.getGameData(gameSlug);
        } catch (ClientErrorException e) {
            System.out.println("Error getting current racetime races:\n" + e.getMessage());
            return new ArrayList<>();
        }
        return currentRaces.getCurrentRaces();
    }
    
    public String getSpectateUrl(String username, String gameSlug) {
        List<Race> currentRaces = getCurrentRaces(gameSlug);
        for (Race race : currentRaces) {
            RaceData raceData;
            try {
                raceData = raceDataProxy.getRaceData(race.getData_url());
            } catch (ClientErrorException e) {
                System.out.println("Error getting racetime race data:\n" + e.getMessage());
                return null;
            }
            for (Entrant entrant : raceData.getEntrants()) {
                if (entrant.getUser().getFullName().equals(username)) {
                    return String.format("%s%s/spectate", BASE_URI, raceData.getUrl());
                }
            }
        }
        return null;
    }
}
