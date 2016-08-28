package ru.nlp_project.story_line2.crawler;

import static com.mongodb.client.model.Filters.*;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDBClientManager {


	private String connectionUrl;
	private MongoClient client;
	private MongoCollection<Document> collection;
	private Logger logger;

	public MongoDBClientManager(String connectionUrl) {
		logger = LoggerFactory.getLogger(this.getClass());
		this.connectionUrl = connectionUrl;
	}

	public static MongoDBClientManager newInstance(CrawlerConfiguration configuration) {
		MongoDBClientManager result = new MongoDBClientManager(configuration.connectionUrl);
		result.initialize();
		return result;
	}

	private void initialize() {
		MongoClientURI mongoClientURI = new MongoClientURI(connectionUrl);
		this.client = new MongoClient(mongoClientURI);
	}

	public void shutdown() {
		client.close();
	}

	/**
	 * Произвести запись в БД для уникальной комбинации domain:path. При наличии подобной записи -
	 * не делать ничего.
	 * 
	 * @param json
	 * @param domain
	 * @param path
	 */
	public void writeNews(String json, String domain, String path) {
		collection = getNewsCollections();
		FindIterable<Document> find = collection.find(and(eq("domain", domain), eq("path", path)));
		if (find.first() != null) {
			String msg =
					String.format("Record for (%s:%s) already exists in MongoDB.", domain, path);
			logger.info(msg);
			// don nothing
			return;
		}

		Document document = Document.parse(json);
		try {
			collection.insertOne(document);
			if (logger.isTraceEnabled()) {
				String msg =
						String.format("Write record to MongoDB for (%s:%s) - '%s'.", domain, path, json);
				logger.trace(msg);
			} else {
				String msg =
						String.format("Write record to MongoDB for (%s:%s).", domain, path, json);
				logger.info(msg);
			}
		} catch (com.mongodb.MongoException e) {
			String msg = String.format("Exception while write record to MongoDB for (%s:%s): %s.",
					domain, path, e.getMessage());
			logger.error(msg);
		}
	}

	private MongoCollection<Document> getNewsCollections() {
		if (collection == null) {
			MongoDatabase database = client.getDatabase("crawler");
			collection = database.getCollection("news");
		}
		return collection;
	}

}
