package ru.nlp_project.story_line2.crawler.impl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
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
import ru.nlp_project.story_line2.crawler.IContentProcessor.DataSourcesEnum;
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
					.tag("service", IMetricsManager.SERVICE)
					// !!! converter
					// al influxdbMetrics must be of form: "in_app.bnkomi_ru.rss.invocation_count" ->
					// measurement name: "invocation_count" with tags [scope=in_app, source=bnkomi_ru, source_type=rss] value=0.1"
					.transformer(new CategoriesMetricMeasurementTransformer("scope", "source", "source_type"))
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
					.tag("service", IMetricsManager.SERVICE)
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


	private Counter getCounter(DataSourcesEnum dataSource, String sourceName, String metricsName) {
		String name = dataSource.toString() + sourceName + metricsName;
		Counter result = counterHashMap.get(name);
		if (result == null) {
			result = createCounter(dataSource, sourceName, metricsName);
			counterHashMap.put(name, result);
		}
		return result;
	}

	private Counter createCounter(DataSourcesEnum dataSource, String sourceName, String metricsName) {
		String name = String
				.format("%s.%s.%s.%s", IMetricsManager.IN_APP_PREFIX,
						sourceName.replace(".", "_"), metricsName, dataSource.toString().toLowerCase());
		return metricRegistry.counter(name);
	}


	@Override
	public void incrementPagesProcessed(DataSourcesEnum dataSource, String sourceName) {
		getCounter(dataSource, sourceName, IMetricsManager.METRIC_NAME_PAGE_PROCESSED).inc();
	}

	@Override
	public void incrementPagesEmpty(DataSourcesEnum dataSource, String sourceName) {
		getCounter(dataSource, sourceName, IMetricsManager.METRIC_NAME_PAGE_EMPTY).inc();
	}

	@Override
	public void incrementPagesFull(DataSourcesEnum dataSource, String sourceName) {
		getCounter(dataSource, sourceName, IMetricsManager.METRIC_NAME_PAGE_FULL).inc();
	}

	@Override
	public void incrementExtractionEmptyPubDate(DataSourcesEnum dataSource, String sourceName) {
		getCounter(dataSource, sourceName, IMetricsManager.METRIC_NAME_EXTRACTED_EMPTY_PUB_DATE).inc();
	}

	@Override
	public void incrementExtractionEmptyTitle(DataSourcesEnum dataSource, String sourceName) {
		getCounter(dataSource, sourceName, IMetricsManager.METRIC_NAME_EXTRACTED_EMPTY_TITLE).inc();

	}

	@Override
	public void incrementExtractionEmptyContent(DataSourcesEnum dataSource, String sourceName) {
		getCounter(dataSource, sourceName, IMetricsManager.METRIC_NAME_EXTRACTED_EMPTY_CONTENT).inc();

	}

	@Override
	public void incrementExtractionEmptyImageUrl(DataSourcesEnum dataSource, String sourceName) {
		getCounter(dataSource, sourceName, IMetricsManager.METRIC_NAME_EXTRACTED_EMPTY_IMAGE_URL).inc();
	}

	@Override
	public void incrementLinkProcessed(DataSourcesEnum dataSource, String sourceName) {
		getCounter(dataSource, sourceName, IMetricsManager.METRIC_NAME_LINK_PROCESSED).inc();
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
