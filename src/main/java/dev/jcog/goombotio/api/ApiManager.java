package dev.jcog.goombotio.api;

import dev.jcog.goombotio.api.bluesky.BlueskyApi;
import dev.jcog.goombotio.api.bttv.BttvApi;
import dev.jcog.goombotio.api.ffz.FfzApi;
import dev.jcog.goombotio.api.racetime.RacetimeApi;
import dev.jcog.goombotio.api.seventv.SevenTvApi;
import dev.jcog.goombotio.api.srcom.SrcApi;
import dev.jcog.goombotio.api.youtube.YoutubeApi;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

public class ApiManager {
    private final FfzApi ffzApi;
    private final BttvApi bttvApi;
    private final SevenTvApi sevenTvApi;
    
    private final BlueskyApi blueskyApi;
    private final RacetimeApi racetimeApi;
    private final SrcApi srcApi;
    private final YoutubeApi youtubeApi;
    
    public ApiManager() {
        ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();
        ffzApi = new FfzApi(client);
        bttvApi = new BttvApi(client);
        sevenTvApi = new SevenTvApi(client);
        
        blueskyApi = new BlueskyApi(client);
        racetimeApi = new RacetimeApi(client);
        srcApi = new SrcApi(client);
        youtubeApi = new YoutubeApi(client);
    }
    
    public FfzApi getFfzApi() {
        return ffzApi;
    }
    
    public BttvApi getBttvApi() {
        return bttvApi;
    }
    
    public SevenTvApi getSevenTvApi() {
        return sevenTvApi;
    }
    
    public BlueskyApi getBlueskyApi() {
        return blueskyApi;
    }
    
    public RacetimeApi getRacetimeApi() {
        return racetimeApi;
    }
    
    public SrcApi getSrcApi() {
        return srcApi;
    }
    
    public YoutubeApi getYoutubeApi() {
        return youtubeApi;
    }
}
