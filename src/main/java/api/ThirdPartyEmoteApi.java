package api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Map;

public class ThirdPartyEmoteApi extends BaseAPI {
    private static final String SEVENTV_URL = "https://7tv.io/v3/users/twitch/138137087";
    private static final String SEVENTV_EMOTE_SET_KEY = "emote_set";
    private static final String SEVENTV_EMOTES_KEY = "emotes";
    private static final String SEVENTV_ID_KEY = "id";
    private static final String SEVENTV_NAME_KEY = "name";

    private static final String BTTV_URL = "https://api.betterttv.net/2/channels/jcog";
    private static final String BTTV_EMOTES_KEY = "emotes";
    private static final String BTTV_ID_KEY = "id";
    private static final String BTTV_CODE_KEY = "code";
    
    public static Map<String, String> get7tvEmotes() {
        JSONParser jsonParser = new JSONParser();
        JSONObject object;
        try {
            object = (JSONObject) jsonParser.parse(submitRequest(SEVENTV_URL));
        } catch (ParseException e) {
            System.out.println("Error getting 7TV emotes");
            e.printStackTrace();
            return new HashMap<>();
        }
        JSONObject set = (JSONObject) object.get(SEVENTV_EMOTE_SET_KEY);
        JSONArray emotesJsonArray = (JSONArray) set.get(SEVENTV_EMOTES_KEY);
        
        Map<String, String> emotesMap = new HashMap<>();
        for (Object emote : emotesJsonArray) {
            String id = ((JSONObject) emote).get(SEVENTV_ID_KEY).toString();
            String name = ((JSONObject) emote).get(SEVENTV_NAME_KEY).toString();
            emotesMap.put(name, id);
        }
        return emotesMap;
    }

    public static Map<String, String> getBttvEmotes() {
        JSONParser jsonParser = new JSONParser();
        JSONObject object;
        try {
            object = (JSONObject) jsonParser.parse(submitRequest(BTTV_URL));
        } catch (ParseException e) {
            System.out.println("Error getting BTTV emotes");
            e.printStackTrace();
            return new HashMap<>();
        }
        JSONArray emotesJsonArray = (JSONArray) object.get(BTTV_EMOTES_KEY);

        Map<String, String> emotesMap = new HashMap<>();
        for (Object emote : emotesJsonArray) {
            String id = ((JSONObject) emote).get(BTTV_ID_KEY).toString();
            String code = ((JSONObject) emote).get(BTTV_CODE_KEY).toString();
            emotesMap.put(code, id);
        }
        return emotesMap;
    }
}
