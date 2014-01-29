package at.tuwien.aic.db;

import com.mongodb.DB;
import com.mongodb.Mongo;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {

    private static Database instance;

    private Mongo mongo;
    private Map<String, DB> dbs;
    private String dbHost;

    private Database() throws UnknownHostException {
        mongo = new Mongo("localhost");
        dbs = new HashMap<String, DB>();
    }

    public DB getDatabase(String dbName) {
        if (!dbs.containsKey(dbName)) {
            dbs.put(dbName, mongo.getDB(dbName));
        }

        return dbs.get(dbName);
    }

    public static Database getInstance() throws UnknownHostException {
        if (instance == null) {
            instance = new Database();
        }

        return instance;
    }
}
