package api;

import api.Bttv.BttvApi;
import api.Ffz.FfzApi;

public class ApiManager {
    private final FfzApi ffzApi;
    private final BttvApi bttvApi;
    
    public ApiManager() {
        ffzApi = new FfzApi();
        bttvApi = new BttvApi();
    }
    
    public FfzApi getFfzApi() {
        return ffzApi;
    }
    
    public BttvApi getBttvApi() {
        return bttvApi;
    }
}
