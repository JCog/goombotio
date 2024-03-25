package api;

import api.Ffz.FfzApi;

public class ApiManager {
    private final FfzApi ffzApi;
    
    public ApiManager() {
        ffzApi = new FfzApi();
    }
    
    public FfzApi getFfzApi() {
        return ffzApi;
    }
}
