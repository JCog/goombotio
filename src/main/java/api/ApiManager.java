package api;

import api.Bttv.BttvApi;
import api.Ffz.FfzApi;
import api.SevenTv.SevenTvApi;

public class ApiManager {
    private final FfzApi ffzApi;
    private final BttvApi bttvApi;
    private final SevenTvApi sevenTvApi;
    
    public ApiManager() {
        ffzApi = new FfzApi();
        bttvApi = new BttvApi();
        sevenTvApi = new SevenTvApi();
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
}
