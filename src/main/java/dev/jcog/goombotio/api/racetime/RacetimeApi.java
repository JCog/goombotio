package dev.jcog.goombotio.api.racetime;

import dev.jcog.goombotio.api.racetime.gamedata.GameData;
import dev.jcog.goombotio.api.racetime.gamedata.GameDataInterface;
import dev.jcog.goombotio.api.racetime.gamedata.Race;
import dev.jcog.goombotio.api.racetime.racedata.Entrant;
import dev.jcog.goombotio.api.racetime.racedata.RaceData;
import dev.jcog.goombotio.api.racetime.racedata.RaceDataInterface;
import jakarta.ws.rs.ClientErrorException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RacetimeApi {
    private static final Logger log = LoggerFactory.getLogger(RacetimeApi.class);
    private static final String BASE_URI = "https://racetime.gg";

    private final GameDataInterface gameDataProxy;
    private final RaceDataInterface raceDataProxy;
    
    public RacetimeApi(ResteasyClient client) {
        ResteasyWebTarget target = client.target(BASE_URI);
        gameDataProxy = target.proxy(GameDataInterface.class);
        raceDataProxy = target.proxy(RaceDataInterface.class);
    }
    
    public List<Race> getCurrentRaces(String gameSlug) {
        GameData currentRaces;
        try {
            currentRaces = gameDataProxy.getGameData(gameSlug);
        } catch (ClientErrorException e) {
            log.error("Error getting current racetime races: {}", e.getMessage());
            return new ArrayList<>();
        }
        return currentRaces.getCurrentRaces();
    }
    
    public String getSpectateUrl(String username, String ... gameSlugs) {
        for (String game : gameSlugs) {
            List<Race> currentRaces = getCurrentRaces(game);
            for (Race race : currentRaces) {
                RaceData raceData;
                try {
                    raceData = raceDataProxy.getRaceData(race.getData_url());
                } catch (ClientErrorException e) {
                    log.error("Error getting racetime race data: {}", e.getMessage());
                    return null;
                }
                for (Entrant entrant : raceData.getEntrants()) {
                    if (entrant.getUser().getFullName().equals(username)) {
                        return String.format("%s%s/spectate", BASE_URI, raceData.getUrl());
                    }
                }
            }
        }
        return null;
    }
}
