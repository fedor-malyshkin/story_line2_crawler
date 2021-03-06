package ru.nlp_project.story_line2.crawler.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class BerkeleyDBDatabaseProcessorTest {

  public static final int COUNT_OF_ITERATIONS = 10_000;
  private BerkeleyDBDatabaseProcessor testable;

  @Before
  public void setUp() throws IOException {
    testable = new BerkeleyDBDatabaseProcessor();
    String absolutePath = Files.createTempDirectory("berkeleyDB").toFile().getAbsolutePath();
    testable.initialize(absolutePath);
  }

  @Test
  public void testInitializeNotExistingDir() throws IOException {
    testable = new BerkeleyDBDatabaseProcessor();
    String absolutePath = FileUtils.getTempDirectoryPath() + File.separator + UUID.randomUUID();
    testable.initialize(absolutePath);
    testable.add("url");
  }


  @Test
  public void test10000RecordsSuccess() {
    for (int i = 0; i < COUNT_OF_ITERATIONS; i++) {
      testable.add("url" + i);
    }

    for (int i = 0; i < COUNT_OF_ITERATIONS; i++) {
      Assertions.assertThat(testable.contains("url" + i)).isTrue();
    }

  }


  @Test
  public void test10000RecordsFail() {
    for (int i = 0; i < COUNT_OF_ITERATIONS; i++) {
      testable.add("url" + i);
    }

    for (int i = 0; i < COUNT_OF_ITERATIONS; i++) {
      Assertions.assertThat(testable.contains("NOurl" + i)).isFalse();
    }
  }

}