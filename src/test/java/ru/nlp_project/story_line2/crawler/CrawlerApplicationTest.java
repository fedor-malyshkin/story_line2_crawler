package ru.nlp_project.story_line2.crawler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;

public class CrawlerApplicationTest {
	private Environment environment = mock(Environment.class);
	private JerseyEnvironment jersey = mock(JerseyEnvironment.class);
	private CrawlerApplication application = new CrawlerApplication();
	private CrawlerConfiguration configuration = new CrawlerConfiguration();
	private HealthCheckRegistry hcRegistry = mock(HealthCheckRegistry.class);

	@Before
	public void setUp() throws Exception {
		// when(environment.jersey()).thenReturn(jersey);

		// configuration
		Path configDir = Files.createTempDirectory("crawler-config");
		FileUtils.forceDeleteOnExit(configDir.toFile());
		FileUtils.copyDirectory(new File("src/test/resources/ru/nlp_project/story_line2/crawler"),
				configDir.toFile(), new SuffixFileFilter("yml"));
		FileUtils.copyDirectory(
				new File("src/main/groovy/ru/nlp_project/story_line2/crawler/parser"),
				configDir.toFile(), new SuffixFileFilter("groovy"));


		ObjectMapper mapper = Jackson.newObjectMapper(new YAMLFactory()); // create once, reuse
		configuration = mapper.readValue(
				new File(configDir.toFile() + File.separator + "CrawlerApplicationTest.yml"),
				CrawlerConfiguration.class);
		configuration.scriptDir = configDir.toFile().toString();
		configuration.storageDir = configDir.toFile().toString();

		// mocks
		when(environment.healthChecks()).thenReturn(hcRegistry);
		// when(hcRegistry.register(anyString(), anyObject()));
	}


	/**
	 * Осуществить дамп всех новостей в файл в каталоге локального хранилища для кравлера.
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void dumpsNewsToFiles() throws Exception {
		application.dumpsNewsToFiles(configuration, environment);
	}
}
