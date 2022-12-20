package database.misc;

import com.mongodb.client.FindIterable;
import database.GbCollection;
import database.GbDatabase;
import database.entries.MinecraftUser;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class MinecraftUserDb extends GbCollection {
    private static final String COLLECTION_NAME = "minecraft_users";
    
    private static final String MC_USERNAME_KEY = "mc_username";
    private static final String MC_UUID_KEY = "mc_uuid";

    public MinecraftUserDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }

    public void addUser(String twitchId, String mcUuid, String mcUsername) {
        Document result = findFirstEquals(ID_KEY, twitchId);
        if (result == null) {
            Document document = new Document(ID_KEY, twitchId)
                    .append(MC_UUID_KEY, mcUuid)
                    .append(MC_USERNAME_KEY, mcUsername);
            insertOne(document);
        } else {
            updateOne(twitchId, new Document(MC_UUID_KEY, mcUuid));
            updateOne(twitchId, new Document(MC_USERNAME_KEY, mcUsername));
        }
    }

    public MinecraftUser getUser(String twitchId) {
        Document result = findFirstEquals(ID_KEY, twitchId);
        return convertMinecraftUser(result);
    }
    
    public ArrayList<MinecraftUser> getAllUsers() {
        FindIterable<Document> documents = findAll();
        ArrayList<MinecraftUser> allUsers = new ArrayList<>();
        for (Document document : documents) {
            allUsers.add(convertMinecraftUser(document));
        }
        return allUsers;
    }

    @Nullable
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
