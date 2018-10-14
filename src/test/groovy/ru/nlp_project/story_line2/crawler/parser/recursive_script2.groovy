package ru.nlp_project.story_line2.crawler.parser

import ru.nlp_project.story_line2.crawler.parser.recursive_script1

import groovy.transform.TypeChecked

public class recursive_script2 {
  public static String source = "recursive_script2"

  @TypeChecked
  def extractData(source, webUrl, html) {
    println(recursive_script1.class)
    return [:]
  }

  def shouldVisit(url) {
    return true;
  }
}
