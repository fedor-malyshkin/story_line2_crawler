package ru.nlp_project.story_line2.crawler;

import com.mongodb.DBObject;

public interface IMongoDBClient {

	public interface IRecordIterationProcessor {
		void processRecord(String domain, String path, String content);

	}


	void shutdown();

	/**
	 * Произвести запись в БД для уникальной комбинации domain:path. При наличии подобной записи -
	 * не делать ничего.
	 * 
	 * @param dbObject
	 * @param domain
	 * @param path
	 */
	void writeNews(DBObject dbObject, String domain, String path);

	void dumpsAllNewsToFiles(IRecordIterationProcessor reader);

}
