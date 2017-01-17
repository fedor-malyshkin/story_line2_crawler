package ru.nlp_project.story_line2.crawler.impl;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

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
		FindIterable<DBObject> find = collection.find(and(eq("domain", domain), eq("path", path)));
		if (find.first() != null) {
			String msg =
					String.format("Record for (%s:%s) already exists in MongoDB.", domain, path);
			logger.debug(msg);
			// don nothing
			return;
		}

		// Document document = Document.parse(json);
		try {
			collection.insertOne(dbObject);
			if (logger.isTraceEnabled()) {
				String msg = String.format("Write record to MongoDB for (%s:%s) - '%s'.", domain,
						path, dbObject);
				logger.trace(msg);
			} else {
				String msg = String.format("Write record to MongoDB for (%s:%s).", domain, path,
						dbObject);
				logger.info(msg);
			}
		} catch (com.mongodb.MongoException e) {
			String msg = String.format("Exception while write record to MongoDB for (%s:%s): %s.",
					domain, path, e.getMessage());
			logger.error(msg);
		}
	}

	private MongoCollection<DBObject> getNewsCollections() {
		if (collection == null) {
			MongoDatabase database = client.getDatabase("crawler");
			collection = database.getCollection("news", DBObject.class);
		}
		return collection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.nlp_project.story_line2.crawler.IMongoDBClient#dumpsAllNewsToFiles(ru.nlp_project.
	 * story_line2.crawler.MongoDBClientManager.IDumpRecordProcessor)
	 */
	@Override
	public void dumpsAllNewsToFiles(IRecordIterationProcessor reader) {
		collection = getNewsCollections();
		FindIterable<DBObject> find = collection.find();
		MongoCursor<DBObject> iterator = find.iterator();
		while (iterator.hasNext()) {
			DBObject doc = iterator.next();
			String domain = (String) doc.get("domain");
			String path = (String) doc.get("path");
			String content = (String) doc.get("content");
			reader.processRecord(domain, path, content);
		}


	}

}
