package ru.nlp_project.story_line2.crawler;

import com.mongodb.DBObject;

public interface IMongoDBClient {
	public static final String DB_NAME = "crawler";
	public static final String COLLECTION_NAME = "crawler_entries";

	void shutdown();

	/**
	 * Произвести запись в БД для уникальной комбинации domain:path. При наличии подобной записи -
	 * не делать ничего.
	 * 
	 * @param dbObject
	 * @param source
	 * @param path
	 */
	void writeNews(DBObject dbObject, String source, String path);

	boolean isNewsExists(String source, String path);
}
