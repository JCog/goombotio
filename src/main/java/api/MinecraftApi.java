package api;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;

public class MinecraftApi extends BaseAPI {
    private static final String BASE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String ID_KEY = "id";
    private static final String NAME_KEY = "name";

    //returns Arraylist of the format [id, name]
    public static ArrayList<String> getProfile(String username) {
        String url = BASE_URL + username;

        JSONParser jsonParser = new JSONParser();
        JSONObject object;
        try {
            object = (JSONObject) jsonParser.parse(submitRequest(url));
        } catch (Exception e) {
            return null;
        }

        String id = object.get(ID_KEY).toString();
        String name = object.get(NAME_KEY).toString();
        ArrayList<String> output = new ArrayList<>();
        output.add(id);
        output.add(name);
        return output;
    }
}
