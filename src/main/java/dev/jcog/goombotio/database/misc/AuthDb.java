package dev.jcog.goombotio.database.misc;

import dev.jcog.goombotio.database.GbCollection;
import dev.jcog.goombotio.database.GbDatabase;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

public class AuthDb extends GbCollection {
    private static final String COLLECTION_NAME = "auth";
    private static final String AUTH_TOKEN_KEY = "auth_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";

    public static class AuthItem {
        public final String id;

        public String authToken;
        public String refreshToken;

        public AuthItem(String id, String authToken, String refreshToken) {
            this.id = id;
            this.authToken = authToken;
            this.refreshToken = refreshToken;
        }
    }

    public AuthDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }

    public void setAuthToken(String id, String authToken, String refreshToken) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result == null) {
            Document document = new Document(ID_KEY, id)
                    .append(AUTH_TOKEN_KEY, authToken)
                    .append(REFRESH_TOKEN_KEY, refreshToken);
            insertOne(document);
        } else {
            updateOne(id, new Document(AUTH_TOKEN_KEY, authToken));
            updateOne(id, new Document(REFRESH_TOKEN_KEY, refreshToken));
        }
    }

    public void setAuthToken(AuthItem authItem) {
        setAuthToken(authItem.id, authItem.authToken, authItem.refreshToken);
    }

    @Nullable
    public AuthItem getAuth(String id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result == null) {
            return null;
        } else {
            return new AuthItem(
                    result.getString(ID_KEY),
                    result.getString(AUTH_TOKEN_KEY),
                    result.getString(REFRESH_TOKEN_KEY)
            );
        }
    }
}
