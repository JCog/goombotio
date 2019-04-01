package Util.Database;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.*;

public abstract class CollectionBase {
    protected GoombotioDb goombotioDb;
    protected MongoCollection<Document> collection;

    public CollectionBase() {
        goombotioDb = GoombotioDb.getInstance();
        collection = setCollection();
    }

    // I feel like there's a better way to do this but idk
    protected abstract MongoCollection<Document> setCollection();

    protected void insertOne(Document document) {
        collection.insertOne(document);
    }

    protected void updateOne(Bson filter, Bson update) {
        collection.updateOne(filter, update);
    }

    protected FindIterable<Document> find(Bson filter) {
        return collection.find(filter);
    }
}
