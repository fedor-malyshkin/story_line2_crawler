package ru.nlp_project.story_line2.crawler.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.bson.BSON;
import org.bson.BSONObject;

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
import ru.nlp_project.story_line2.crawler.model.Id;

/**
 * Утилита для серипизации/десериализации данных BSON формат.
 * 
 * WARN: Поля, являющиеся идентификаторами (или ссылками на идентификаторы) необходимо делать типа
 * {@link ru.nlp_project.story_line2.server_storm.model.Id}.
 * 
 * @author fedor
 *
 */
public class BSONUtils {


	public static class BsonDateDeserializer extends JsonDeserializer<Date> {

		@Override
		public Date deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			if (p instanceof BsonParser) {
				BsonParser bsonParser = (BsonParser) p;
				if (bsonParser.getCurrentToken() != JsonToken.VALUE_EMBEDDED_OBJECT
						|| bsonParser.getCurrentBsonType() != BsonConstants.TYPE_DATETIME) {
					throw ctxt.mappingException(Date.class.getCanonicalName());
				}
				return (Date) bsonParser.getEmbeddedObject();
			} else {
				return new Date(p.getLongValue());
			}
		}

	}


	public static class BsonDateSerializer extends JsonSerializer<Date> {
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


	public static class BsonIdDeserializer extends JsonDeserializer<Id> {

		@Override
		public Id deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			if (p instanceof BsonParser) {
				BsonParser bsonParser = (BsonParser) p;
				if (bsonParser.getCurrentToken() != JsonToken.VALUE_EMBEDDED_OBJECT
						|| bsonParser.getCurrentBsonType() != BsonConstants.TYPE_OBJECTID) {
					throw ctxt.mappingException(ObjectId.class.getCanonicalName());
				}
				ObjectId tempId = (ObjectId) bsonParser.getEmbeddedObject();
				return new Id(getHexString(tempId));
			} else {
				TreeNode tree = p.getCodec().readTree(p);
				int time = ((ValueNode) tree.get("$time")).asInt();
				int machine = ((ValueNode) tree.get("$machine")).asInt();
				int inc = ((ValueNode) tree.get("$inc")).asInt();
				return new Id(getHexString(new ObjectId(time, machine, inc)));
			}
		}

	}


	public static class BsonIdSerializer extends JsonSerializer<Id> {

		@Override
		public void serialize(final Id id, final JsonGenerator gen,
				final SerializerProvider provider) throws IOException {

			if (gen instanceof BsonGenerator) {
				BsonGenerator bgen = (BsonGenerator) gen;
				if (id == null)
					bgen.writeNull();
				else
					bgen.writeObjectId(createObjectId(id.value));
			} else {
				gen.writeNumber(createObjectId(id.value).getTime());
			}
		}
	}


	private static ObjectMapper bsonMapper;


	public static <T> T deserialize(DBObject item, Class<T> class1) {
		ObjectMapper objectMapper = getBSONObjectMapper();
		byte[] encode = BSON.encode(item);
		try {
			return objectMapper.readValue(encode, class1);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static BasicDBObject serialize(Object instance) {
		ObjectMapper mapper = getBSONObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			mapper.writeValue(baos, instance);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		BSONObject decode = BSON.decode(baos.toByteArray());
		return new BasicDBObject(decode.toMap());
	}



	/**
	 * <ul>
	 * <li>See: http://stackoverflow.com/questions/12463049/date-format-mapping-to-json-jackson</li>
	 * See:
	 * <li>https://github.com/michel-kraemer/bson4jackson/tree/master/src/main/java/de/undercouch/bson4jackson</li>
	 * </ul>
	 * 
	 * @return
	 */
	public static ObjectMapper getBSONObjectMapper() {
		if (bsonMapper == null) {
			BsonFactory bsonFactory = new BsonFactory();
			bsonFactory.enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH);
			bsonMapper = new ObjectMapper(bsonFactory);
			bsonMapper.setSerializationInclusion(Include.NON_NULL);


			final SimpleModule module = new SimpleModule("", Version.unknownVersion());
			module.addSerializer(Date.class, new BsonDateSerializer());
			module.addDeserializer(Date.class, new BsonDateDeserializer());
			module.addSerializer(Id.class, new BsonIdSerializer());
			module.addDeserializer(Id.class, new BsonIdDeserializer());
			bsonMapper.registerModule(module);
		}
		return bsonMapper;
	}



	public static String getHexString(ObjectId objectId) {
		if (objectId == null)
			throw new IllegalArgumentException("objectId must not be null");
		// TODO: rewrite serializer/deserializer for modern format
		org.bson.types.ObjectId objectIdInt = org.bson.types.ObjectId.createFromLegacyFormat(
				objectId.getTime(), objectId.getMachine(), objectId.getInc());
		return objectIdInt.toHexString();
	}

	// Big-Endian helpers, in this class because all other BSON numbers are little-endian

	public static int makeInt(final byte b3, final byte b2, final byte b1, final byte b0) {
		// CHECKSTYLE:OFF
		return (((b3) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff)));
		// CHECKSTYLE:ON
	}


	public static ObjectId createObjectId(String hexString) {
		if (!org.bson.types.ObjectId.isValid(hexString))
			throw new IllegalArgumentException(
					"hexString is not valid - may be its time to rewrite for moder ObjectId format!&");
		org.bson.types.ObjectId bsonObjectId = new org.bson.types.ObjectId(hexString);
		byte[] b = bsonObjectId.toByteArray();
		// such complex way because bytes in native Object id is mapped to 4 components - machine,
		// time, process, counter
		return new ObjectId(makeInt(b[0], b[1], b[2], b[3]), makeInt(b[4], b[5], b[6], b[7]),
				makeInt(b[8], b[9], b[10], b[11]));
	}

	public static org.bson.types.ObjectId createBsonObjectId(Id id) {
		if (id == null)
			throw new IllegalArgumentException("objectId must not be null");
		return new org.bson.types.ObjectId(id.value);
	}

}
