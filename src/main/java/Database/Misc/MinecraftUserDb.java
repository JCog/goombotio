package Database.Misc;

import Database.CollectionBase;
import Database.Entries.MinecraftUser;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class MinecraftUserDb extends CollectionBase {
    private static final String COLLECTION_NAME_KEY = "minecraft_users";
    private static final String MC_USERNAME_KEY = "mc_username";
    private static final String MC_UUID_KEY = "mc_uuid";

    private static MinecraftUserDb instance = null;

    private MinecraftUserDb() {
        super();
    }

    public static MinecraftUserDb getInstance() {
        if (instance == null) {
            instance = new MinecraftUserDb();
        }
        return instance;
    }

    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME_KEY);
    }

    public void addUser(String twitchId, String mcUuid, String mcUsername) {
        Document result = findFirstEquals(ID_KEY, twitchId);
        if (result == null) {
            Document document = new Document(ID_KEY, twitchId)
                    .append(MC_UUID_KEY, mcUuid)
                    .append(MC_USERNAME_KEY, mcUsername);
            insertOne(document);
        }
        else {
            updateOne(twitchId, new Document(MC_UUID_KEY, mcUuid));
            updateOne(twitchId, new Document(MC_USERNAME_KEY, mcUsername));
        }
    }

    public MinecraftUser getUser(String twitchId) {
        Document result = findFirstEquals(ID_KEY, twitchId);
        return convertMinecraftUser(result);
    }

    private MinecraftUser convertMinecraftUser(Document userDoc) {
        if (userDoc == null) {
            return null;
        }
        String twitchId = userDoc.getString(ID_KEY);
        String mcUuid = userDoc.getString(MC_UUID_KEY);
        String mcUsername = userDoc.getString(MC_USERNAME_KEY);
        return new MinecraftUser(twitchId, mcUuid, mcUsername);
    }
}
