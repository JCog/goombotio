package api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;

public class FfzBttvApi extends BaseAPI {
    private static final String FFZ_URL = "https://api.frankerfacez.com/v1/room/jcog";
    private static final String FFZ_SETS_KEY = "sets";
    private static final String FFZ_SET_ID_KEY = "170319";
    private static final String FFZ_EMOTICONS_KEY = "emoticons";
    private static final String FFZ_ID_KEY = "id";
    private static final String FFZ_NAME_KEY = "name";

    private static final String BTTV_URL = "https://api.betterttv.net/2/channels/jcog";
    private static final String BTTV_EMOTES_KEY = "emotes";
    private static final String BTTV_ID_KEY = "id";
    private static final String BTTV_CODE_KEY = "code";

    public static HashMap<String, String> getFfzEmotes() {
        JSONParser jsonParser = new JSONParser();
        JSONObject object;
        try {
            object = (JSONObject) jsonParser.parse(submitRequest(FFZ_URL));
        }
        catch (ParseException e) {
            System.out.println("Error getting FFZ emotes");
            e.printStackTrace();
            return new HashMap<>();
        }
        JSONObject sets = (JSONObject) object.get(FFZ_SETS_KEY);
        JSONObject set = (JSONObject) sets.get(FFZ_SET_ID_KEY);
        JSONArray emotesJsonArray = (JSONArray) set.get(FFZ_EMOTICONS_KEY);

        HashMap<String, String> emotesMap = new HashMap<>();
        for (Object emote : emotesJsonArray) {
            String id = ((JSONObject) emote).get(FFZ_ID_KEY).toString();
            String name = ((JSONObject) emote).get(FFZ_NAME_KEY).toString();
            emotesMap.put(name, id);
        }
        return emotesMap;
    }

    public static HashMap<String, String> getBttvEmotes() {
        JSONParser jsonParser = new JSONParser();
        JSONObject object;
        try {
            object = (JSONObject) jsonParser.parse(submitRequest(BTTV_URL));
        }
        catch (ParseException e) {
            System.out.println("Error getting BTTV emotes");
            e.printStackTrace();
            return new HashMap<>();
        }
        JSONArray emotesJsonArray = (JSONArray) object.get(BTTV_EMOTES_KEY);

        HashMap<String, String> emotesMap = new HashMap<>();
        for (Object emote : emotesJsonArray) {
            String id = ((JSONObject) emote).get(BTTV_ID_KEY).toString();
            String code = ((JSONObject) emote).get(BTTV_CODE_KEY).toString();
            emotesMap.put(code, id);
        }
        return emotesMap;
    }
}
