package api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class RacetimeApi extends BaseAPI {
    private static final String BASE_URL = "https://racetime.gg";
    private static final String DATA_URL = "/data";
    private static final String SPECTATE_URL = "/spectate";
    
    public static String getSpectateUrl(String username, String gameSlug) {
        JSONParser jsonParser = new JSONParser();
        JSONObject gameData;
        try {
            gameData = (JSONObject) jsonParser.parse(submitRequest(BASE_URL + '/' + gameSlug + DATA_URL));
        } catch (Exception e) {
            return null;
        }
        
        JSONArray currentRaces = (JSONArray) gameData.get("current_races");
        for (Object raceObject : currentRaces) {
            JSONObject race = (JSONObject) raceObject;
            String raceDataUrl = race.get("data_url").toString();
            JSONObject raceData;
            try {
                raceData = (JSONObject) jsonParser.parse(submitRequest(BASE_URL + raceDataUrl));
            } catch (Exception e) {
                return null;
            }
            
            JSONArray entrants = (JSONArray) raceData.get("entrants");
            for (Object entrantObject : entrants) {
                JSONObject entrant = (JSONObject) entrantObject;
                JSONObject user = (JSONObject) entrant.get("user");
                String fullName = user.get("full_name").toString();
                if (fullName.equals(username)) {
                    String raceUrl = raceData.get("url").toString();
                    return BASE_URL + raceUrl + SPECTATE_URL;
                }
            }
        }
        
        return null;
    }
}
