package ru.nlp_project.story_line2.crawler.dagger;

import javax.inject.Singleton;

import dagger.Component;
import ru.nlp_project.story_line2.crawler.Crawler;
import ru.nlp_project.story_line2.crawler.NewsWebCrawler;

@Component(modules = ApplicationModule.class)
@Singleton
public interface ApplicationComponent {
	void inject(Crawler crawler);

	void inject(NewsWebCrawler crawler);

}
