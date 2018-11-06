package ru.nlp_project.story_line2.crawler;

import com.sleepycat.je.DatabaseException;

public interface IDatabaseProcessor {

  public void initialize(String datastorePath);

  public boolean contains(String uri);

  public void add(String uri);
}
