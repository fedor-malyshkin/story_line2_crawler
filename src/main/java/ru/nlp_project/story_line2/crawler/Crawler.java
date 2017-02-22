package ru.nlp_project.story_line2.crawler;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.quartz.Scheduler;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;

import io.dropwizard.lifecycle.Managed;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.FeedSiteConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.ParseSiteConfiguration;
import ru.nlp_project.story_line2.crawler.dagger.CrawlerBuilder;
import ru.nlp_project.story_line2.crawler.feed_site.FeedSiteController;
import ru.nlp_project.story_line2.crawler.parse_site.ParseSiteController;

/**
 * Крулер - основной класс бизнес-логики и построения компонентов.
 * 
 * @author fedor
 *
 */
public class Crawler implements Managed {
	public static final String METRICS_PREFIX = "crawler.";

	public static Crawler newInstance(CrawlerConfiguration configuration) throws Exception {
		Crawler result = new Crawler(configuration);
		CrawlerBuilder.setCrawlerConfiguration(configuration);
		CrawlerBuilder.getComponent().inject(result);
		return result;
	}

	@Inject
	protected Scheduler scheduler;

	@Inject
	protected MetricRegistry metricRegistry;

	private CrawlerConfiguration configuration;

	private List<ParseSiteController> parseSites;

	private List<FeedSiteController> feedSites;

	// only for shutdowning
	@Inject
	protected IMongoDBClient dbClientManager;

	private Crawler(CrawlerConfiguration configuration) {
		this.configuration = configuration;
	}

	private void initializeSites() throws Exception {
		// parser_sites
		parseSites = new ArrayList<>();
		if (null != configuration.parseSites)
			for (ParseSiteConfiguration siteConfig : configuration.parseSites) {
				parseSites.add(new ParseSiteController(siteConfig));
			}

		// feed_sites
		feedSites = new ArrayList<>();
		if (null != configuration.feedSites)
			for (FeedSiteConfiguration siteConfig : configuration.feedSites) {
				feedSites.add(new FeedSiteController(siteConfig));
			}

		// initialize
		parseSites.forEach(s -> s.initialize());
		feedSites.forEach(s -> s.initialize());
		// start
		parseSites.forEach(s -> s.start());
		feedSites.forEach(s -> s.start());
	}

	// see:
	// http://stackoverflow.com/questions/875467/java-client-certificates-over-https-ssl/876785#876785
	private void initializeAllTrustCert() {
		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] certs, String authType) {}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {}

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
		} catch (Exception e) {
		}

	}


	private void initializeMetricsLogging() {
		final Slf4jReporter reporter = Slf4jReporter.forRegistry(metricRegistry)
				.outputTo(LoggerFactory.getLogger("ru.nlp_project.story_line2.crawler"))
				.convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
		reporter.start(1, TimeUnit.MINUTES);

		final JmxReporter reporter2 = JmxReporter.forRegistry(metricRegistry).build();
		reporter2.start();

	}


	@Override
	public void start() throws Exception {
		// time to startup service
		scheduler.startDelayed(10);
		initializeMetricsLogging();
		initializeAllTrustCert();
		initializeSites();
	}


	@Override
	public void stop() throws Exception {
		scheduler.shutdown(false);
		dbClientManager.shutdown();
		// start
		parseSites.forEach(s -> s.stop());
		feedSites.forEach(s -> s.stop());
	}


}
