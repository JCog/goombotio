package api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

public abstract class BaseAPI {
    protected static final OkHttpClient client = new OkHttpClient();

    protected static String submitRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        } catch (IOException | NullPointerException e) {
            return null;
        }
    }
}
