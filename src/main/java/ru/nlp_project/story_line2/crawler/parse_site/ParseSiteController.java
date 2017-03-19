package ru.nlp_project.story_line2.crawler.parse_site;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.CrawlController.WebCrawlerFactory;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.ParseSiteConfiguration;
import ru.nlp_project.story_line2.crawler.dagger.CrawlerBuilder;

public class ParseSiteController {
	@DisallowConcurrentExecution
	public static class ParseSiteJon implements Job {

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			// JobKey key = context.getJobDetail().getKey();
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			String source = dataMap.getString("source");
			ParseSiteController siteController = controllers.get(source);
			try {
				if (siteController != null)
					siteController.executeSheduledJob(context);
			} catch (Exception e) {
				// do nothing in any case
				// see:
				// http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-03.html
			}
		}

	}



	// global controllers list for shedule processing
	public static Map<String, ParseSiteController> controllers = new HashMap<>();


	@Inject
	protected Scheduler scheduler;

	@Inject
	protected CrawlerConfiguration crawlerConfiguration;

	private ParseSiteConfiguration siteConfig;

	private Logger logger;

	private CrawlController crawlController;

	private JobDetail job;

	private CronTrigger trigger;

	private JobKey jobKey;

	WebCrawlerFactory<ParseSiteCrawler> factory = new WebCrawlerFactory<ParseSiteCrawler>() {
		@Override
		public ParseSiteCrawler newInstance() throws Exception {
			ParseSiteCrawler result = new ParseSiteCrawler(siteConfig);
			result.initialize();
			return result;
		}
	};

	public ParseSiteController(ParseSiteConfiguration siteConfig) {
		this.siteConfig = siteConfig;
		CrawlerBuilder.getComponent().inject(this);
		this.logger = LoggerFactory.getLogger(this.getClass());
		// put in global controllers list (for quartz jobs)
		controllers.put(siteConfig.source, this);
	}

	private void checkScriptsDirectory() {
		try {
			Collection<File> files = FileUtils.listFiles(new File(crawlerConfiguration.scriptDir),
					new String[] {"groovy"}, true);
			if (files.size() == 0) {
				logger.error("Empty script directory {} ", crawlerConfiguration.scriptDir);
				throw new IllegalStateException(
						"Empty script directory: " + crawlerConfiguration.scriptDir);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IllegalStateException(e);
		}
	}

	private CrawlConfig createCrawlConfig(ParseSiteConfiguration site) {
		String crawlStorageFolder = crawlerConfiguration.storageDir + File.separator + site.source;

		try {
			FileUtils.forceMkdir(new File(crawlStorageFolder));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new IllegalStateException(e);
		}

		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(crawlStorageFolder);
		config.setResumableCrawling(true);
		config.setPolitenessDelay(100);
		return config;
	}

	private CrawlController createCrawlController(CrawlConfig crawlConfig,
			ParseSiteConfiguration site) throws Exception {
		/*
		 * Instantiate the controller for this crawl.
		 */
		PageFetcher pageFetcher = new PageFetcher(crawlConfig);
		RobotstxtConfig robotsTxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotsTxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(crawlConfig, pageFetcher, robotstxtServer);

		/*
		 * For each crawl, you need to add some seed urls. These are the first URLs that are fetched
		 * and then the crawler starts following links which are found in these pages
		 */
		controller.addSeed(site.seed);
		return controller;
	}

	public void executeSheduledJob(JobExecutionContext context) {
		// rerun crawler if is finished (may be there is new pages)
		if (crawlController.isFinished())
			startCrawler();
	}

	public void initialize() {
		checkScriptsDirectory();
		initializeScheduleJob();
		initializeCrawler();
	}

	private void initializeCrawler() {
		CrawlConfig crawlConfig = createCrawlConfig(siteConfig);
		try {
			crawlController = createCrawlController(crawlConfig, siteConfig);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}


	private void initializeScheduleJob() {
		jobKey = new JobKey("Job:" + siteConfig.source, "parse_site");
		// define the job
		job = newJob(ParseSiteJon.class).withIdentity(jobKey)
				.usingJobData("source", siteConfig.source).build();

		CronScheduleBuilder scheduleBuilder =
				cronSchedule(siteConfig.cronSchedule).withMisfireHandlingInstructionDoNothing();

		trigger = newTrigger().withIdentity("Trigger:" + siteConfig.source, "parse_site")
				.withSchedule(scheduleBuilder).forJob(job).build();

	}

	public void start() {
		// Tell quartz to schedule the job using our trigger
		try {
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException e) {
			logger.error("There is error while scheduling; {}", siteConfig.source, e);
			throw new IllegalStateException(e);
		}

		startCrawler();
	}

	private void startCrawler() {
		if (crawlerConfiguration.async)
			crawlController.startNonBlocking(factory, crawlerConfiguration.crawlerPerSite);
		else
			crawlController.start(factory, crawlerConfiguration.crawlerPerSite);
	}

	public void stop() {
		try {
			scheduler.deleteJob(jobKey);
		} catch (SchedulerException e) {
		}
		crawlController.shutdown();
		crawlController.waitUntilFinish();
	}

}
