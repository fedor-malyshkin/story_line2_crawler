package ru.nlp_project.story_line2.crawler.dagger;

import javax.inject.Singleton;

import dagger.Component;
import ru.nlp_project.story_line2.crawler.NewsWebCrawler;

@Component(modules = ApplicationModule.class)
@Singleton
public abstract class ApplicationComponent {
	public abstract void inject(NewsWebCrawler crawler);

}
