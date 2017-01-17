package ru.nlp_project.story_line2.crawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.bson.BSON;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import de.undercouch.bson4jackson.BsonConstants;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonGenerator;
import de.undercouch.bson4jackson.BsonParser;
import de.undercouch.bson4jackson.types.ObjectId;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * Краулер для новостей.
 * 
 * @author fedor
 *
 */
public class NewsWebCrawler extends WebCrawler {
	public static class MyDateSerializer extends JsonSerializer<Date> {

		@Override
		public void serialize(final Date value, final JsonGenerator gen,
				final SerializerProvider provider) throws IOException {

			if (gen instanceof BsonGenerator) {
				BsonGenerator bgen = (BsonGenerator) gen;
				if (value == null)
					bgen.writeNull();
				else
					bgen.writeDateTime(value);
			} else {
				gen.writeNumber(value.getTime());
			}
		}
	}


	public static class MyDateDeserializer extends JsonDeserializer<Date> {

		@Override
		public Date deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			if (p instanceof BsonParser) {
				BsonParser bsonParser = (BsonParser) p;
				if (bsonParser.getCurrentToken() != JsonToken.VALUE_EMBEDDED_OBJECT
						|| bsonParser.getCurrentBsonType() != BsonConstants.TYPE_DATETIME) {
					throw ctxt.mappingException(Date.class);
				}
				return (Date) bsonParser.getEmbeddedObject();
			} else {
				return new Date(p.getLongValue());
			}
		}

	}


	public static class MyObjectIdSerializer extends JsonSerializer<ObjectId> {

		@Override
		public void serialize(final ObjectId value, final JsonGenerator gen,
				final SerializerProvider provider) throws IOException {
			if (gen instanceof BsonGenerator) {
				BsonGenerator bgen = (BsonGenerator) gen;
				if (value == null)
					bgen.writeNull();
				else
					bgen.writeObject(value);
			} else {
				gen.writeNumber(value.getTime());
			}
		}
	}


	public static class MyObjectIdDeserializer extends JsonDeserializer<ObjectId> {

		@Override
		public ObjectId deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			if (p instanceof BsonParser) {
				BsonParser bsonParser = (BsonParser) p;
				if (bsonParser.getCurrentToken() != JsonToken.VALUE_EMBEDDED_OBJECT
						|| bsonParser.getCurrentBsonType() != BsonConstants.TYPE_OBJECTID) {
					throw ctxt.mappingException(ObjectId.class);
				}
				return (ObjectId) bsonParser.getEmbeddedObject();
			} else {
				TreeNode tree = p.getCodec().readTree(p);
				int time = ((ValueNode) tree.get("$time")).asInt();
				int machine = ((ValueNode) tree.get("$machine")).asInt();
				int inc = ((ValueNode) tree.get("$inc")).asInt();
				return new ObjectId(time, machine, inc);
			}
		}

	}


	@Inject
	public IGroovyInterpreter groovyInterpreter;
	@Inject
	public CrawlerConfiguration configuration;
	@Inject
	public IMongoDBClient dbClientManager;
	private ObjectMapper mapper;
	private Logger myLogger;

	public NewsWebCrawler() {
		myLogger = LoggerFactory.getLogger(this.getClass());
	}

	/**
	 * This method receives two parameters. The first parameter is the page in which we have
	 * discovered this new url and the second parameter is the new url. You should implement this
	 * function to specify whether the given url should be crawled or not (based on your crawling
	 * logic). In this example, we are instructing the crawler to ignore urls that have css, js,
	 * git, ... extensions and to only accept urls that start with "http://www.ics.uci.edu/". In
	 * this case, we didn't need the referringPage parameter to make the decision.
	 */
	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		// в случае если страница будет отвергнута -- она не будет проанализирована и самой
		// библиотекой
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
				String msg = String.format("[%s] '%s' has no content.", webURL.getDomain(),
						webURL.getURL());
				myLogger.debug(msg);
				return;
			}

			try {
				DBObject dbObject =
						serialize(webURL.getDomain().toLowerCase(), webURL.getPath().toLowerCase(),
								webURL.getURL().toLowerCase(), (Date) data.get("date"), new Date(),
								data.get("title").toString(), data.get("content").toString());
				dbClientManager.writeNews(dbObject, webURL.getDomain(), webURL.getPath());
			} catch (IOException e) {
				myLogger.error(e.getMessage(), e);
			}
		}
	}

	public ObjectMapper getObjectMapper() {
		if (mapper == null) {
			// mapper = new ObjectMapper(new JsonFactory());
			// look in http://www.michel-kraemer.com/binary-json-with-bson4jackson
			BsonFactory bsonFactory = new BsonFactory();
			bsonFactory.enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH);
			mapper = new ObjectMapper(bsonFactory);
			final SimpleModule module = new SimpleModule("", Version.unknownVersion());
			module.addSerializer(Date.class, new MyDateSerializer());
			module.addDeserializer(Date.class, new MyDateDeserializer());
			module.addSerializer(ObjectId.class, new MyObjectIdSerializer());
			module.addDeserializer(ObjectId.class, new MyObjectIdDeserializer());
			mapper.registerModule(module);
			// not serialize null values
			mapper.setSerializationInclusion(Include.NON_NULL);

		}
		return mapper;
	}

	private DBObject serialize(String domain, String path, String url, Date date, Date creationDate,
			String title, String content) throws IOException {
		ObjectMapper mapper = getObjectMapper();
		CrawlerNewsArticle article =
				new CrawlerNewsArticle(creationDate, date, content, path, domain, title, url);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mapper.writeValue(baos, article);
		BSONObject decode = BSON.decode(baos.toByteArray());
		Map map = decode.toMap();
		// map.put("date", new BsonDateTime(creationDate.getTime()));
		return new BasicDBObject(map);
	}

}
