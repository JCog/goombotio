package Database;

import Util.Settings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static java.lang.System.out;

public abstract class CollectionBase {
    protected static final String ID_KEY = "_id";
    
    protected final GoombotioDb goombotioDb = GoombotioDb.getInstance();
    
    private final MongoCollection<Document> collection = setCollection();
    private final boolean writePermission = Settings.hasWritePermission();
    

    protected CollectionBase() {
    
    }

    // I feel like there's a better way to do this but idk
    protected abstract MongoCollection<Document> setCollection();

    /*
    Inserts the given document into the collection
     */
    protected void insertOne(Document document) {
        if (writePermission) {
            collection.insertOne(document);
        }
        else {
            out.println("DATABASE: attempted to insertOne");
        }
    }
    
    /*
    Updates a single document in the collection according to the given document
     */
    protected void updateOne(long id, Document document) {
        if (writePermission) {
            collection.updateOne(eq(ID_KEY, id), new Document("$set", document));
        }
        else {
            out.println("DATABASE: attempted to updateOne");
        }
    }
    
    /*
    Updates a single document in the collection according to the given document
     */
    protected void updateOne(String id, Document document) {
        if (writePermission) {
            collection.updateOne(eq(ID_KEY, id), new Document("$set", document));
        }
        else {
            out.println("DATABASE: attempted to updateOne");
        }
    }
    
    /*
    Removes the document with the given id from the collection if it exists
     */
    protected void deleteOne(long id) {
        if (writePermission) {
            collection.deleteOne(eq(ID_KEY, id));
        }
        else {
            out.println("DATABASE: attempted to deleteOne");
        }
    }
    
    /*
    Removes the document with the given id from the collection if it exists
     */
    protected void deleteOne(String id) {
        if (writePermission) {
            collection.deleteOne(eq(ID_KEY, id));
        }
        else {
            out.println("DATABASE: attempted to deleteOne");
        }
    }

    /*
    Finds all documents in the collection
     */
    protected FindIterable<Document> findAll() {
        return collection.find();
    }
    
    /*
    Finds the first document where the value of the key name equals the specified value
     */
    protected Document findFirstEquals(String key, String value) {
        return collection.find(eq(key, value)).first();
    }
    
    /*
    Finds the first document where the value of the key name equals the specified value
     */
    protected Document findFirstEquals(String key, long value) {
        return collection.find(eq(key, value)).first();
    }
    
    /*
    Finds all documents in the collection that contain the given field
     */
    protected FindIterable<Document> findContainsKey(String key) {
        return collection.find(exists(key));
    }
    
    /*
    Returns the number of documents in the collection
     */
    protected long countDocuments() {
        return collection.countDocuments();
    }
}
