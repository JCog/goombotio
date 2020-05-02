package APIs;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.text.NumberFormat;

public class YoutubeApi extends BaseAPI {
    private static final String urlPattern = "https://www.googleapis.com/youtube/v3/videos?id=%s&part=snippet,statistics&key=%s";
    
    private static final String ITEMS_KEY = "items";
    private static final String SNIPPET_KEY = "snippet";
    private static final String TITLE_KEY = "title";
    private static final String CHANNEL_TITLE_KEY = "channelTitle";
    private static final String STATISTICS_KEY = "statistics";
    private static final String VIEW_COUNT_KEY = "viewCount";
    private static final String LIKE_COUNT_KEY = "likeCount";
    private static final String DISLIKE_COUNT_KEY = "dislikeCount";
    
    public static String getVideoDetails(String id, String apiKey) {
        JSONParser jsonParser = new JSONParser();
        JSONObject object;
        try {
            object = (JSONObject) jsonParser.parse(submitRequest(buildUrlString(id, apiKey)));
        } catch (ParseException e) {
            System.out.println("Error getting youtube data");
            e.printStackTrace();
            return "";
        }
        
        String title;
        String channelName;
        int viewCount;
        int likeCount;
        int dislikeCount;
        
        JSONArray items = (JSONArray) object.get(ITEMS_KEY);
        JSONObject item = (JSONObject) items.get(0);
        JSONObject snippet = (JSONObject) item.get(SNIPPET_KEY);
        title = snippet.get(TITLE_KEY).toString();
        channelName = snippet.get(CHANNEL_TITLE_KEY).toString();
        
        JSONObject stats = (JSONObject) item.get(STATISTICS_KEY);
        viewCount = Integer.parseInt(stats.get(VIEW_COUNT_KEY).toString());
        likeCount = Integer.parseInt(stats.get(LIKE_COUNT_KEY).toString());
        dislikeCount = Integer.parseInt(stats.get(DISLIKE_COUNT_KEY).toString());
    
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);
        return String.format(
                "YouTube Video: %s • %s • %s views | \uD83D\uDC4D%s | \uD83D\uDC4E%s",
                channelName,
                title,
                numberFormat.format(viewCount),
                numberFormat.format(likeCount),
                numberFormat.format(dislikeCount)
        );
    }
    
    
    private static String buildUrlString(String id, String apiKey) {
        return String.format(urlPattern, id, apiKey);
    }
}
