package ru.nlp_project.story_line2.crawler.impl;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import java.io.File;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import ru.nlp_project.story_line2.crawler.IDatabaseProcessor;

@Slf4j
public class BerkeleyDBDatabaseProcessor implements IDatabaseProcessor {

  public static final byte[] EMPTY_STRING_BYTES = "".getBytes(StandardCharsets.UTF_8);
  private Database database = null;

  @Override
  public void initialize(String datastorePath) {
    try {
      initializeDirectory(datastorePath);
      Environment dbEnvironment = initializeEnvironment(datastorePath);
      initializeDatabase(dbEnvironment);
    } catch (DatabaseException e) {
      log.error("Exception in BerkeleyDB: {}", e.getMessage(), e);
      throw new IllegalStateException(e);
    }
  }

  private void initializeDirectory(String datastorePath) {
    File file = new File(datastorePath);
    if (file.exists() && !file.isDirectory()) {
      throw new IllegalStateException(String.format("'%s' must be not file.", datastorePath));
    }
    if (!file.exists() && !file.mkdirs()) {
      throw new IllegalStateException(String.format(" Cannot create '%s' dir.", datastorePath));
    }
  }

  private void initializeDatabase(Environment dbEnvironment) throws DatabaseException {
    // Open the database, creating one if it does not exist
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setAllowCreate(true);
    database = dbEnvironment.openDatabase(null,
                                          "crawler", dbConfig);
  }

  private Environment initializeEnvironment(String datastorePath) throws DatabaseException {
    // Open the environment, creating one if it does not exist
    EnvironmentConfig envConfig = new EnvironmentConfig();
    envConfig.setAllowCreate(true);
    return new Environment(new File(datastorePath),
                           envConfig);
  }

  @Override
  public boolean contains(String uri) {
    try {
      DatabaseEntry theKey = new DatabaseEntry(uri.getBytes(StandardCharsets.UTF_8));
      DatabaseEntry theData = new DatabaseEntry();
      OperationStatus operationStatus = database.get(null, theKey, theData, LockMode.DEFAULT);
      return operationStatus == OperationStatus.SUCCESS;
    } catch (DatabaseException e) {
      log.error("Exception in BerkeleyDB: {}", e.getMessage(), e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void add(String uri) {
    try {
      DatabaseEntry theKey = new DatabaseEntry(uri.getBytes(StandardCharsets.UTF_8));
      DatabaseEntry theData = new DatabaseEntry(EMPTY_STRING_BYTES);
      database.put(null, theKey, theData);
    } catch (DatabaseException e) {
      log.error("Exception in BerkeleyDB: {}", e.getMessage(), e);
      throw new IllegalStateException(e);
    }
  }
}
