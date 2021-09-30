package database;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import static com.mongodb.client.model.Filters.*;
import static java.lang.System.out;

public abstract class GbCollection {
    protected static final String ID_KEY = "_id";

    protected final GbDatabase gbDatabase;

    private final MongoCollection<Document> collection;
    private final boolean writePermission;


    protected GbCollection(GbDatabase gbDatabase) {
        this.gbDatabase = gbDatabase;
        writePermission = gbDatabase.hasWritePermission();
        collection = gbDatabase.getCollection(getCollectionName());
    }

    protected abstract String getCollectionName();

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
    Finds all documents in the collection who's value of key contains query as a substring
     */
    protected FindIterable<Document> findContainsSubstring(String key, String query, boolean caseSensitive) {
        if (caseSensitive) {
            return collection.find(regex(key, ".*" + query + ".*"));
        }
        else {
            return collection.find(regex(key, ".*" + query + ".*", "i"));
        }
    }

    /*
    Returns the number of documents in the collection
     */
    protected long countDocuments() {
        return collection.countDocuments();
    }

    /*
    Sorts documents in iterable in ascending order by the sortField
     */
    protected FindIterable<Document> sortAscending(FindIterable<Document> iterable, String sortField) {
        return iterable.sort(Sorts.ascending(sortField));
    }

    /*
    Sorts documents in iterable in descending order by the sortField
     */
    protected FindIterable<Document> sortDescending(FindIterable<Document> iterable, String sortField) {
        return iterable.sort(Sorts.descending(sortField));
    }
}
