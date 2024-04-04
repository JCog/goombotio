package database;

import com.mongodb.MongoNamespace;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.lang.Nullable;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.*;
import static java.lang.System.out;

public abstract class GbCollection {
    protected static final String ID_KEY = "_id";

    protected final GbDatabase gbDatabase;

    private final MongoCollection<Document> collection;
    private final boolean writePermission;


    protected GbCollection(GbDatabase gbDatabase, String collectionName) {
        this.gbDatabase = gbDatabase;
        writePermission = gbDatabase.hasWritePermission();
        collection = gbDatabase.getCollection(collectionName);
    }

    /*
    Inserts the given document into the collection
     */
    protected void insertOne(Document document) {
        if (writePermission) {
            collection.insertOne(document);
        } else {
            out.printf("%s: attempted to insert %s\n", collection.getNamespace(), document);
        }
    }
    
    /*
    Updates a single document in the collection according to the given document
     */
    protected void updateOne(Object id, Document document) {
        if (writePermission) {
            collection.updateOne(eq(ID_KEY, id), new Document("$set", document));
        } else {
            out.printf("%s: attempted to update %s with %s\n", collection.getNamespace(), id, document);
        }
    }
    
    /*
    Updates the field of a single document in the collection to value
     */
    protected void updateField(Object id, String fieldName, Object value) {
        if (writePermission) {
            Bson filter = eq(ID_KEY, id);
            Bson update = Updates.set(fieldName, value);
            collection.updateOne(filter, update);
        } else {
            out.printf(
                    "%s: attempted to set id=\"%s\" fieldName=\"%s\" value=\"%s\"\n",
                    collection.getNamespace(),
                    id,
                    fieldName,
                    value
            );
        }
    }
    
    /*
    Adds an item to the specified arrayName. The array will be created first if it doesn't exist.
     */
    protected void pushItemToArray(Object id, String arrayName, Object value) {
        if (writePermission) {
            Bson filter = and(eq(ID_KEY, id), exists(arrayName, false));
            Bson update = Updates.set(arrayName, new ArrayList<>());
            collection.updateOne(filter, update);
            
            filter = eq(ID_KEY, id);
            update = Updates.push(arrayName, value);
            collection.updateOne(filter, update);
        } else {
            out.printf(
                    "%s: attempted to push id=\"%s\" arrayName=\"%s\" value=\"%s\"\n",
                    collection.getNamespace(),
                    id,
                    arrayName,
                    value
            );
        }
    }

    /*
    Removes the document with the given id from the collection if it exists
     */
    protected void deleteOne(Object id) {
        if (writePermission) {
            collection.deleteOne(eq(ID_KEY, id));
        } else {
            out.printf("%s: attempted to delete %s\n", collection.getNamespace(), id);
        }
    }
    
    /*
    Removes an item matching value from arrayName in the given id
     */
    protected void pullItemFromArray(Object id, String arrayName, Object value) {
        if (writePermission) {
            collection.updateOne(eq(ID_KEY, id), Updates.pull(arrayName, value));
        } else {
            out.printf(
                    "%s: attempted to pull id=\"%s\" arrayName=\"%s\" value=\"%s\"\n",
                    collection.getNamespace(),
                    id,
                    arrayName,
                    value
            );
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
    protected @Nullable Document findFirstEquals(String key, Object value) {
        return collection.find(eq(key, value)).first();
    }
    
    /*
    Finds all documents where the value of the key name equals the specified value
     */
    protected FindIterable<Document> findEquals(String key, Object value) {
        return collection.find(eq(key, value));
    }

    /*
    Finds all documents in the collection that contain the given field
     */
    protected FindIterable<Document> findContainsKey(String key) {
        return collection.find(exists(key));
    }

    /*
    Finds all documents in the collection whose key contains query as a substring
     */
    protected FindIterable<Document> findContainsSubstring(String key, String query, boolean caseSensitive) {
        if (caseSensitive) {
            return collection.find(regex(key, ".*" + query + ".*"));
        } else {
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
    
    /*
    Renames the current collection to the specified name
     */
    protected void renameCollection(String name) {
        collection.renameCollection(new MongoNamespace(collection.getNamespace().getDatabaseName(), name));
    }
}
