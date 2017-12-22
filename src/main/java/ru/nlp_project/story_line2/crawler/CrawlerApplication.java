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
import ru.nlp_project.story_line2.crawler.impl.MetricsManagerImpl;
import ru.nlp_project.story_line2.crawler.impl.MongoDBClientImpl;
import ru.nlp_project.story_line2.crawler.parse_site.ParseSiteController;

/**
 * @author fedor
 */

@SpringBootApplication // same as @Configuration @EnableAutoConfiguration @ComponentScan
@EnableConfigurationProperties(CrawlerConfiguration.class)
public class CrawlerApplication {

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
	public IMongoDBClient mongoDBClient(CrawlerConfiguration configuration) {
		return MongoDBClientImpl.newInstance(configuration);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	IContentProcessor contentProcessor() {
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
	public void start(CrawlerConfiguration configuration) throws Exception {
		// time to startup service
		scheduler().startDelayed(10);
		initializeAllTrustCert();
		initializeSites(configuration);
	}

	@PreDestroy
	public void stop(CrawlerConfiguration configuration) throws Exception {
		scheduler().shutdown(false);
		mongoDBClient(configuration).shutdown();
		// start
		parseSites.forEach(ParseSiteController::stop);
		feedSites.forEach(FeedSiteController::stop);
	}

	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@Bean
	FeedSiteController feedSiteController(FeedSiteConfiguration configuration) {
		FeedSiteController result = new FeedSiteController(configuration);
		result.initialize();
		return result;
	}


	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@Bean
	ParseSiteController parseSiteController(ParseSiteConfiguration configuration) {
		ParseSiteController result = new ParseSiteController(configuration);
		result.initialize();
		return result;
	}

	private void initializeSites(
			CrawlerConfiguration configuration) throws Exception {
		// parser_sites
		parseSites = new ArrayList<>();
		if (null != configuration.parseSites) {
			for (ParseSiteConfiguration siteConfig : configuration.parseSites) {
				parseSites.add(parseSiteController(siteConfig));
			}
		}

		// feed_sites
		feedSites = new ArrayList<>();
		if (null != configuration.feedSites) {
			for (FeedSiteConfiguration siteConfig : configuration.feedSites) {
				feedSites.add(feedSiteController(siteConfig));
			}
		}

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

