package ru.nlp_project.story_line2.crawler.impl;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;

/**
 * Клиент mongoDB для сохранения в БД.
 * 
 * 
 * @author fedor
 *
 */
public class MongoDBClientImpl implements IMongoDBClient {


	private String connectionUrl;
	private MongoClient client;
	private MongoCollection<DBObject> collection;
	private Logger logger;

	public MongoDBClientImpl(String connectionUrl) {
		logger = LoggerFactory.getLogger(this.getClass());
		this.connectionUrl = connectionUrl;
	}

	public static MongoDBClientImpl newInstance(CrawlerConfiguration configuration) {
		MongoDBClientImpl result = new MongoDBClientImpl(configuration.connectionUrl);
		result.initialize();
		return result;
	}

	private void initialize() {
		MongoClientURI mongoClientURI = new MongoClientURI(connectionUrl);
		this.client = new MongoClient(mongoClientURI);
		// create index
		MongoCollection<DBObject> collections = getNewsCollections();
		BasicDBObject obj = new BasicDBObject();
		obj.put("source", 1);
		obj.put("path", 1);
		// uniques + bckg
		IndexOptions ndx = new IndexOptions();
		ndx.background(true);
		ndx.unique(true);
		collections.createIndex(obj, ndx);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.nlp_project.story_line2.crawler.IMongoDBClient#shutdown()
	 */
	@Override
	public void shutdown() {
		client.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.nlp_project.story_line2.crawler.IMongoDBClient#writeNews(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void writeNews(DBObject dbObject, String domain, String path) {
		collection = getNewsCollections();
		FindIterable<DBObject> find =
				collection.find(and(eq("source", domain), eq("path", path))).limit(1);
		if (find.first() != null) {
			logger.debug("Record already exists {}:{}", domain, path);
			// don nothing
			return;
		}

		// Document document = Document.parse(json);
		try {
			collection.insertOne(dbObject);
			if (logger.isTraceEnabled()) {
				logger.trace("Write record {}:{} - '{}'.", domain, path, dbObject);
			} else {
				logger.info("Write record {}:{}.", domain, path);
			}
		} catch (Exception e) {
			logger.error("Exception while write record {}:{}", domain, path, e);
		}
	}

	private MongoCollection<DBObject> getNewsCollections() {
		if (collection == null) {
			MongoDatabase database = client.getDatabase(IMongoDBClient.DB_NAME);
			collection = database.getCollection(IMongoDBClient.COLLECTION_NAME, DBObject.class);
		}
		return collection;
	}

	@Override
	public boolean isNewsExists(String source, String path) {
		collection = getNewsCollections();
		FindIterable<DBObject> find =
				collection.find(and(eq("source", source), eq("path", path))).limit(1);
		if (find.first() != null)
			return true;
		return false;
	}


}
