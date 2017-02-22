package ru.nlp_project.story_line2.crawler.dagger;


import javax.inject.Singleton;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;

import com.codahale.metrics.MetricRegistry;
import com.mongodb.DBObject;

import dagger.Module;
import dagger.Provides;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IImageLoader;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;
import ru.nlp_project.story_line2.crawler.impl.GroovyInterpreterImpl;
import ru.nlp_project.story_line2.crawler.impl.ImageLoaderImpl;

@Module
public class CrawlerModule {
	CrawlerConfiguration configuration;
	private MetricRegistry metricRegistry;

	public CrawlerModule(CrawlerConfiguration configuration, MetricRegistry metricRegistry) {
		super();
		this.configuration = configuration;
		this.metricRegistry = metricRegistry;
	}

	@Provides
	@Singleton
	public IGroovyInterpreter provideGroovyInterpreter() {
		return GroovyInterpreterImpl.newInstance(configuration);
	}

	@Provides
	@Singleton
	public IMongoDBClient provideMongoDBClient() {
		// return MongoDBClientImpl.newInstance(configuration);
		return new IMongoDBClient() {

			@Override
			public void shutdown() {}

			@Override
			public void writeNews(DBObject dbObject, String source, String path) {}

			@Override
			public boolean isNewsExists(String source, String path) {
				return false;
			}



		};
	}

	@Provides
	public CrawlerConfiguration provideCrawlerConfiguration() {
		return configuration;
	}


	
	@Provides
	public MetricRegistry provideMetricRegistry() {
		return metricRegistry;
	}
	
	@Singleton
	@Provides
	public IImageLoader provideImageLoader() {
		return new ImageLoaderImpl();
	}

	@Singleton
	@Provides
	public Scheduler provideScheduler() {
		try {
			SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
			return schedFact.getScheduler();
		} catch (SchedulerException e) {
			throw new IllegalStateException(e);
		}
	}



}
