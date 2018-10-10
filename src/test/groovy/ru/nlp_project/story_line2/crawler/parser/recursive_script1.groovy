package ru.nlp_project.story_line2.crawler.parser

public class RecursiveScript1 {
  public static String source = "recursive_script1"

  def extractData(source, webUrl, html) {
    println(RecursiveScript2.class)
    return [:]
  }

  def shouldVisit(url) {
    return true;
  }
}
