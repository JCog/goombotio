package Util.Database;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;

public abstract class CollectionBase {
    GoombotioDb goombotioDb;
    private MongoCollection<Document> collection;

    CollectionBase() {
        goombotioDb = GoombotioDb.getInstance();
        collection = setCollection();
    }

    // I feel like there's a better way to do this but idk
    protected abstract MongoCollection<Document> setCollection();

    void insertOne(Document document) {
        collection.insertOne(document);
    }

    void updateOne(Bson filter, Bson update) {
        collection.updateOne(filter, update);
    }
    
    void deleteOne(Bson filter) {
        collection.deleteOne(filter);
    }

    FindIterable<Document> find(Bson filter) {
        return collection.find(filter);
    }

    FindIterable<Document> find() {
        return collection.find();
    }
}
