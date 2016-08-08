package ru.nlp_project.story_line2.crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class NewsWebCrawler extends WebCrawler {

  private GroovyInterpreter groovyInterpreter;
  private ConfigurationReader configurationReader;

  public NewsWebCrawler(GroovyInterpreter groovyInterpreter,
      ConfigurationReader configurationReader) {
    this.groovyInterpreter = groovyInterpreter;
    this.configurationReader = configurationReader;
  }

  /**
   * This method receives two parameters. The first parameter is the page in which we have
   * discovered this new url and the second parameter is the new url. You should implement this
   * function to specify whether the given url should be crawled or not (based on your crawling
   * logic). In this example, we are instructing the crawler to ignore urls that have css, js, git,
   * ... extensions and to only accept urls that start with "http://www.ics.uci.edu/". In this case,
   * we didn't need the referringPage parameter to make the decision.
   */
  @Override
  public boolean shouldVisit(Page referringPage, WebURL url) {
    return groovyInterpreter.shouldVisit(url.getDomain(), url);
  }

  /**
   * This function is called when a page is fetched and ready to be processed by your program.
   */
  @Override
  public void visit(Page page) {
    WebURL webURL = page.getWebURL();
    if (page.getParseData() instanceof HtmlParseData) {
      HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
      String html = htmlParseData.getHtml();

      Map<String, Object> data = groovyInterpreter.extractData(webURL.getDomain(), html);
      if (null == data) {
        System.out.println(webURL.getURL() + " has no content.");
        return;
      }

      if (configurationReader.getConfigurationMain().storeFiles) {
        try {
          File file = new File("/var/tmp/" + System.currentTimeMillis() + ".json");
          FileOutputStream outputStream = new FileOutputStream(file);
          ObjectMapper mapper = new ObjectMapper();
          Map<String, String> map = new HashMap<>();

          map.put("domain", webURL.getDomain());
          map.put("url", webURL.getURL().toLowerCase());
          map.put("date", data.get("date").toString());
          map.put("content", data.get("content").toString());
          map.put("title", data.get("title").toString());

          // content = StringEscapeUtils.escapeJson(data.get("content"));
          mapper.writeValue(outputStream, map);
          IOUtils.closeQuietly(outputStream);
          System.out.println("Create '" + file + "' file");
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target("http://localhost:8080/news");
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON_TYPE);

        // mapping
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = new HashMap<>();

        map.put("domain", webURL.getDomain());
        map.put("url", webURL.getURL().toLowerCase());
        map.put("date", data.get("date").toString());
        map.put("content", data.get("content").toString());
        map.put("title", data.get("title").toString());
        String json = "";
        try {
          json = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }

        // call
        Entity<String> entity = Entity.entity(json, MediaType.APPLICATION_JSON_TYPE);
        Response response = invocationBuilder.post(entity);
      }
    }
  }


}
