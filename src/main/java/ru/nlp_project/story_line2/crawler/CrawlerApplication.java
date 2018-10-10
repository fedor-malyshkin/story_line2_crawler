package ru.nlp_project.story_line2.crawler;

import com.codahale.metrics.MetricRegistry;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.FeedSiteConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.ParseSiteConfiguration;
import ru.nlp_project.story_line2.crawler.feed_site.FeedSiteController;
import ru.nlp_project.story_line2.crawler.impl.ContentProcessorImpl;
import ru.nlp_project.story_line2.crawler.impl.GroovyInterpreterImpl;
import ru.nlp_project.story_line2.crawler.impl.KafkaProducerImpl;
import ru.nlp_project.story_line2.crawler.impl.MetricsManagerImpl;
import ru.nlp_project.story_line2.crawler.parse_site.ParseSiteController;

/**
 * @author fedor
 */

@SpringBootApplication // same as @Configuration @EnableAutoConfiguration @ComponentScan
@EnableConfigurationProperties(CrawlerConfiguration.class)
public class CrawlerApplication {

  @Autowired
  CrawlerConfiguration crawlerConfiguration;

  private List<ParseSiteController> parseSites;

  private List<FeedSiteController> feedSites;

  public static void main(String[] args) throws Exception {
    SpringApplication.run(CrawlerApplication.class, args);
  }

  @Bean
  public IGroovyInterpreter groovyInterpreter(CrawlerConfiguration configuration) {
    return GroovyInterpreterImpl.newInstance(configuration);
  }

  @Bean
  public IKafkaProducer kafkaProducer(CrawlerConfiguration configuration) {
    return KafkaProducerImpl.newInstance(configuration);
  }


  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public IContentProcessor contentProcessor() {
    return new ContentProcessorImpl();
  }

  @Bean
  protected IMetricsManager metricsManager(CrawlerConfiguration configuration,
      MetricRegistry metricRegistry) {
    IMetricsManager result = new MetricsManagerImpl(configuration, metricRegistry);
    result.initialize();
    return result;
  }

  @Bean
  public Scheduler scheduler() {
    try {
      SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
      return schedFact.getScheduler();
    } catch (SchedulerException e) {
      throw new IllegalStateException(e);
    }
  }

  @PostConstruct
  public void start() throws Exception {
    initializeAllTrustCert();
    initializeSites(crawlerConfiguration);
    // time to startup service
    scheduler().startDelayed(10);
  }

  @PreDestroy
  public void stop() throws Exception {
    scheduler().shutdown(false);
    kafkaProducer(crawlerConfiguration).shutdown();
    // start
    parseSites.forEach(ParseSiteController::stop);
    feedSites.forEach(FeedSiteController::stop);
  }

  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Bean
  FeedSiteController feedSiteController(FeedSiteConfiguration siteConfiguration) {
    return new FeedSiteController(siteConfiguration, contentProcessor());
  }


  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Bean
  ParseSiteController parseSiteController(ParseSiteConfiguration siteConfiguration) {
    return new ParseSiteController(siteConfiguration, contentProcessor());
  }

  private void initializeSites(
      CrawlerConfiguration configuration) throws Exception {
    // parser_sites (created after dependency injections, but not initialized)
    parseSites = new ArrayList<>();
    if (null != configuration.parseSites) {
      for (ParseSiteConfiguration siteConfig : configuration.parseSites) {
        parseSites.add(parseSiteController(siteConfig));
      }
    }

    // feed_sites (created after dependency injections, but not initialized)
    feedSites = new ArrayList<>();
    if (null != configuration.feedSites) {
      for (FeedSiteConfiguration siteConfig : configuration.feedSites) {
        feedSites.add(feedSiteController(siteConfig));
      }
    }

    // start
    parseSites.forEach(ParseSiteController::initialize);
    feedSites.forEach(FeedSiteController::initialize);

    // start
    parseSites.forEach(ParseSiteController::start);
    feedSites.forEach(FeedSiteController::start);
  }

  // see:
  // http://stackoverflow.com/questions/875467/java-client-certificates-over-https-ssl/876785#876785
  private void initializeAllTrustCert() {
    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }

      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    }};

    // Ignore differences between given hostname and certificate hostname
    HostnameVerifier hv = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };

    // Install the all-trusting trust manager
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(hv);
    } catch (Exception ignored) {
    }

  }

}

