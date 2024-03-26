package api;

import api.bttv.BttvApi;
import api.ffz.FfzApi;
import api.racetime.RacetimeApi;
import api.seventv.SevenTvApi;
import api.src.SrcApi;
import api.youtube.YoutubeApi;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

public class ApiManager {
    private final FfzApi ffzApi;
    private final BttvApi bttvApi;
    private final SevenTvApi sevenTvApi;
    
    private final RacetimeApi racetimeApi;
    private final SrcApi srcApi;
    private final YoutubeApi youtubeApi;
    
    public ApiManager() {
        ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();
        ffzApi = new FfzApi(client);
        bttvApi = new BttvApi(client);
        sevenTvApi = new SevenTvApi(client);
        
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
