package ru.nlp_project.story_line2.crawler.parser

import ru.nlp_project.story_line2.crawler.parser.recursive_script2

import groovy.transform.TypeChecked

public class recursive_script1 {
  public static String source = "recursive_script1"

  @TypeChecked
  def extractData(source, webUrl, html) {
    println(recursive_script2.class)
    return [:]
  }

  def shouldVisit(url) {
    return true;
  }
}
