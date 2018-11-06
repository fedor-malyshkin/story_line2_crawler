package ru.nlp_project.story_line2.crawler.feed_site;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
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
import org.springframework.beans.factory.annotation.Autowired;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.FeedSiteConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor;
import ru.nlp_project.story_line2.crawler.IDatabaseProcessor;

public class FeedSiteController {

  // global controllers list for schedule processing
  private static Map<String, FeedSiteController> controllers = new HashMap<>();

  @Autowired
  @Setter
  private IDatabaseProcessor databaseProcessor;

  @Autowired
  @Setter
  private IContentProcessor contentProcessor;

  @Autowired
  @Setter
  protected Scheduler scheduler;

  @Autowired
  @Setter
  protected CrawlerConfiguration crawlerConfiguration;

  private FeedSiteConfiguration siteConfig;
  private Logger logger;
  private JobDetail job;
  private CronTrigger trigger;
  private JobKey jobKey;
  private FeedSiteCrawler feedSiteCrawler;


  public FeedSiteController(FeedSiteConfiguration siteConfig) {
    this.siteConfig = siteConfig;
    String loggerClass = String
        .format("%s[%s]", this.getClass().getCanonicalName(), siteConfig.source);
    this.logger = LoggerFactory.getLogger(loggerClass);
    // put in global controllers list (for quartz jobs)
    controllers.put(siteConfig.source, this);
  }

  private void executeScheduledJob(JobExecutionContext context) {
    feedSiteCrawler.crawlFeed();

  }

  public void initialize() {
    initializeScheduleJob();
    initializeFeedSiteCrawler();
  }

  private void initializeFeedSiteCrawler() {
    feedSiteCrawler = new FeedSiteCrawler(crawlerConfiguration, siteConfig, databaseProcessor, contentProcessor);
    feedSiteCrawler.initialize();
  }

  private void initializeScheduleJob() {
    jobKey = new JobKey("Job:" + siteConfig.source, "feed_site");
    // define the job
    job = newJob(FeedSiteJon.class).withIdentity(jobKey)
        .usingJobData("source", siteConfig.source).build();

    CronScheduleBuilder scheduleBuilder =
        cronSchedule(siteConfig.cronSchedule).withMisfireHandlingInstructionDoNothing();

    trigger = newTrigger().startNow().withIdentity("Trigger:" + siteConfig.source, "feed_site")
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


  }

  public void stop() {
    try {
      scheduler.deleteJob(jobKey);
    } catch (SchedulerException e) {
    }

  }

  @DisallowConcurrentExecution
  public static class FeedSiteJon implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      // JobKey key = context.getJobDetail().getKey();
      JobDataMap dataMap = context.getJobDetail().getJobDataMap();
      String source = dataMap.getString("source");
      FeedSiteController siteController = controllers.get(source);
      try {
        if (siteController != null) {
          siteController.executeScheduledJob(context);
        }
      } catch (Exception e) {
        // do nothing in any case
        // see: http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-03.html
      }
    }

  }


}
