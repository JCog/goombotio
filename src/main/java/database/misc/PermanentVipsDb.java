package database.misc;

import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

import java.util.ArrayList;

public class PermanentVipsDb extends GbCollection {
    private static final String COLLECTION_NAME_KEY = "permenant_vips";
    
    public PermanentVipsDb(GbDatabase gbDatabase) {
        super(gbDatabase);
    }
    
    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME_KEY;
    }
    
    public void addVip(String userId) {
        if (findFirstEquals(ID_KEY, userId) == null) {
            Document document = new Document(ID_KEY, userId);
            insertOne(document);
        }
    }
    
    public boolean deleteVip(String userId) {
        if (findFirstEquals(ID_KEY, userId) == null) {
            return false;
        }
        deleteOne(userId);
        return true;
    }
    
    public boolean isPermanentVip(String userId) {
        return findFirstEquals(ID_KEY, userId) != null;
    }
    
    public ArrayList<String> getAllVipUserIds() {
        return findAll().map(document -> document.getString(ID_KEY)).into(new ArrayList<>());
    }
}
