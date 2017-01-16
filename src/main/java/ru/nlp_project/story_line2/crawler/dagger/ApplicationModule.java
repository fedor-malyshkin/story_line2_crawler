package ru.nlp_project.story_line2.crawler.dagger;


import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;
import ru.nlp_project.story_line2.crawler.impl.GroovyInterpreterImpl;
import ru.nlp_project.story_line2.crawler.impl.MongoDBClientImpl;

@Module

public class ApplicationModule {
	CrawlerConfiguration configuration;

	public ApplicationModule(CrawlerConfiguration configuration) {
		super();
		this.configuration = configuration;
	}

	@Provides
	@Singleton
	public IGroovyInterpreter provideGroovyInterpreter() {
		return GroovyInterpreterImpl.newInstance(configuration);
	}
	
	@Provides
	@Singleton
	public IMongoDBClient provideMongoDBClient() {
		return MongoDBClientImpl.newInstance(configuration);
	}
	
	@Provides
	public CrawlerConfiguration provideCrawlerConfiguration() {
		return configuration;
	}

}
