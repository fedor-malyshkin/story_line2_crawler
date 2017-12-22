package ru.nlp_project.story_line2.crawler.impl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.InfluxdbReporter;
import metrics_influxdb.api.measurements.CategoriesMetricMeasurementTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.MetricsConfiguration;
import ru.nlp_project.story_line2.crawler.IMetricsManager;

/**
 * Менеджер метрик.
 *
 * NOTE:
 * В связи с тем, что Spring Boot формирует наименование метрик в подобном ключе:
 * <ul>
 * <li>gauge.response.error</li>
 * <li>...</li>
 * <li>mem.free</li>
 * <ul/>
 *
 * А я по себственной схеме, но так как я хочу их все иметь, мне их нужно разделять.
 * Это возможно с использованием префикса (например "in_app").
 */
public class MetricsManagerImpl implements IMetricsManager {


	private final CrawlerConfiguration configuration;
	private final MetricRegistry metricRegistry;
	private final Logger log;
	private HashMap<String, Timer> timerHashMap = new HashMap<>();
	private HashMap<String, Counter> counterHashMap = new HashMap<>();
	private ScheduledReporter inAppInfluxDBReporter;
	private ScheduledReporter sysInfluxDBReporter;
	private Slf4jReporter slfjReporter;

	public MetricsManagerImpl(CrawlerConfiguration configuration,
			MetricRegistry metricRegistry) {
		log = LoggerFactory.getLogger(this.getClass());
		this.configuration = configuration;
		this.metricRegistry = metricRegistry;
	}

	private void initializeMetricsLogging() throws UnknownHostException {
		MetricsConfiguration metricsConfiguration = configuration.getInfluxdbMetrics();
		System.out.println(configuration);
		System.out.println(metricsConfiguration);

		slfjReporter = Slf4jReporter.forRegistry(metricRegistry)
				.outputTo(LoggerFactory.getLogger("ru.nlp_project.story_line2.server_web"))
				.convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
		slfjReporter.start(metricsConfiguration.logReportingPeriod, TimeUnit.SECONDS);

		if (metricsConfiguration.enabled) {
			String hostName = InetAddress.getLocalHost().getCanonicalHostName();
			inAppInfluxDBReporter = InfluxdbReporter.forRegistry(metricRegistry)
					.protocol(new HttpInfluxdbProtocol("http", metricsConfiguration.influxdbHost,
							metricsConfiguration.influxdbPort, metricsConfiguration.influxdbUser,
							metricsConfiguration.influxdbPassword, metricsConfiguration.influxdbDb))
					// rate + dim conversions
					.convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS)
					// filter
					.filter(new PrefixMetricFilter(IMetricsManager.IN_APP_PREFIX, true))
					// don't skip
					.skipIdleMetrics(false)
					// hostname tag
					.tag("host", hostName)
					.tag("service", "server_web")
					// !!! converter
					// al influxdbMetrics must be of form: "in_app.getImage.bnkomi_ru.invocation_count" ->
					// measurement name: "invocation_count" with tags [scope=in_app, source=bnkomi_ru, method=getImage] value=0.1"
					.transformer(new CategoriesMetricMeasurementTransformer("scope", "method", "source"))
					.build();
			inAppInfluxDBReporter.start(metricsConfiguration.reportingPeriod, TimeUnit.SECONDS);

			sysInfluxDBReporter = InfluxdbReporter.forRegistry(metricRegistry)
					.protocol(new HttpInfluxdbProtocol("http", metricsConfiguration.influxdbHost,
							metricsConfiguration.influxdbPort, metricsConfiguration.influxdbUser,
							metricsConfiguration.influxdbPassword, metricsConfiguration.influxdbDb))
					// rate + dim conversions
					.convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS)
					// filter
					.filter(new PrefixMetricFilter(IMetricsManager.IN_APP_PREFIX, false))
					// don't skip
					.skipIdleMetrics(false)
					// hostname tag
					.tag("host", hostName)
					.tag("service", "server_web")
					.tag("scope", "sys")
					// !!! converter
					// al influxdbMetrics must be of form: "gauge.response.news_articles.article_id" ->
					// measurement name: "gauge.response.news_articles.article_id" with tags [] value=0.1"
					.build();
			sysInfluxDBReporter.start(metricsConfiguration.reportingPeriod, TimeUnit.SECONDS);
		}
	}

	@Override
	public void initialize() {
		try {
			initializeJVMMetrics();
			initializeMetricsLogging();
		} catch (UnknownHostException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void initializeJVMMetrics() {
		/*metricRegistry.register("jvm.memory", new MemoryUsageGaugeSet());
		metricRegistry.register("jvm.thread-states", new ThreadStatesGaugeSet());
		metricRegistry.register("jvm.garbage-collector", new GarbageCollectorMetricSet());*/
	}

	@Override
	@PreDestroy
	public void shutdown() {
		slfjReporter.stop();
		inAppInfluxDBReporter.stop();
		sysInfluxDBReporter.stop();
	}

	@Override
	public void incrementInvocation(String method, String source) {
		Counter counter = getCounter(method, source);
		counter.inc();
	}

	private Counter getCounter(String method, String source) {
		Counter result = counterHashMap.get(method + "-" + source);
		if (result == null) {
			result = createCounter(method, source);
			counterHashMap.put(method + "-" + source, result);
		}
		return result;
	}

	private Counter createCounter(String method, String source) {
		String name = String
				.format("%s.%s.%s.invocation_counter", IMetricsManager.IN_APP_PREFIX, method,
						source.replace(".", "_"));
		return metricRegistry.counter(name);
	}

	@Override
	public void durationInvocation(String method, String source, long duration) {
		Timer timer = getTimer(method, source);
		timer.update(duration, TimeUnit.MILLISECONDS);
	}

	@Override
	public void incrementPagesProcessed(String sourceName) {

	}

	@Override
	public void incrementPagesEmpty(String sourceName) {

	}

	@Override
	public void incrementPagesFull(String sourceName) {

	}

	@Override
	public void incrementExtractionEmptyPubDate(String sourceName) {

	}

	@Override
	public void incrementExtractionEmptyTitle(String sourceName) {

	}

	@Override
	public void incrementExtractionEmptyContent(String sourceName) {

	}

	@Override
	public void incrementExtractionEmptyImageUrl(String sourceName) {

	}

	@Override
	public void incrementLinkProcessed(String sourceName) {

	}

	private Timer getTimer(String method, String source) {
		Timer result = timerHashMap.get(method + "-" + source);
		if (result == null) {
			result = createTimer(method, source);
			timerHashMap.put(method + "-" + source, result);
		}
		return result;
	}

	private Timer createTimer(String method, String source) {
		String name = String
				.format("%s.%s.%s.invocation_duration", IMetricsManager.IN_APP_PREFIX, method,
						source.replace(".", "_"));
		return metricRegistry.timer(name);
	}

	private class PrefixMetricFilter implements MetricFilter {

		private boolean matchPrefix = true;
		private String prefix = IMetricsManager.IN_APP_PREFIX;

		PrefixMetricFilter(String prefix, boolean matchPrefix) {
			this.prefix = prefix;
			this.matchPrefix = matchPrefix;
		}

		@Override
		public boolean matches(String name, Metric metric) {
			return matchPrefix == name.startsWith(prefix);
		}
	}
}
