package api;

import api.bttv.BttvApi;
import api.ffz.FfzApi;
import api.racetime.RacetimeApi;
import api.seventv.SevenTvApi;

public class ApiManager {
    private final FfzApi ffzApi;
    private final BttvApi bttvApi;
    private final SevenTvApi sevenTvApi;
    
    private final RacetimeApi racetimeApi;
    
    public ApiManager() {
        ffzApi = new FfzApi();
        bttvApi = new BttvApi();
        sevenTvApi = new SevenTvApi();
        
        racetimeApi = new RacetimeApi();
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
}
