package api;

import api.bttv.BttvApi;
import api.ffz.FfzApi;
import api.racetime.RacetimeApi;
import api.seventv.SevenTvApi;
import api.src.SrcApi;
import api.youtube.YoutubeApi;

public class ApiManager {
    private final FfzApi ffzApi;
    private final BttvApi bttvApi;
    private final SevenTvApi sevenTvApi;
    
    private final RacetimeApi racetimeApi;
    private final SrcApi srcApi;
    private final YoutubeApi youtubeApi;
    
    public ApiManager() {
        ffzApi = new FfzApi();
        bttvApi = new BttvApi();
        sevenTvApi = new SevenTvApi();
        
        racetimeApi = new RacetimeApi();
        srcApi = new SrcApi();
        youtubeApi = new YoutubeApi();
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
