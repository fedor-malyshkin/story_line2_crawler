package ru.nlp_project.story_line2.crawler.parse_site;

import javax.inject.Inject;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.ParseSiteConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor;
import ru.nlp_project.story_line2.crawler.dagger.CrawlerBuilder;

/**
 * Краулер для новостей.
 * 
 * @author fedor
 *
 */
public class ParseSiteCrawler extends WebCrawler {

	@Inject
	protected IContentProcessor contentProcessor;
	private ParseSiteConfiguration siteConfig;

	ParseSiteCrawler(ParseSiteConfiguration siteConfig) {
		this.siteConfig = siteConfig;
	}

	void initialize() {
		CrawlerBuilder.getComponent().inject(this);
		contentProcessor.initialize(siteConfig.source);
	}


	/**
	 * This method receives two parameters. The first parameter is the page in which we have
	 * discovered this new url and the second parameter is the new url. You should implement this
	 * function to specify whether the given url should be crawled or not (based on your crawling
	 * logic). In this example, we are instructing the crawler to ignore urls that have css, js,
	 * git, ... extensions and to only accept urls that start with "http://www.ics.uci.edu/". In
	 * this case, we didn't need the referringPage parameter to make the decision.
	 */
	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		// в случае если страница будет отвергнута -- она не будет проанализирована и самой
		// библиотекой
		return contentProcessor.shouldVisit(url);
	}

	/**
	 * This function is called when a page is fetched and ready to be processed by your program.
	 */
	@Override
	public void visit(Page page) {
		WebURL webURL = page.getWebURL();
		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String html = htmlParseData.getHtml();

			contentProcessor.processHtml(webURL, html, null, null, null);
		}
	}

}
