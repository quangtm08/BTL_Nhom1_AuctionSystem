package com.nhom1.auction.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.nhom1.auction.common.protocol.ResponseMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Shared JSON helper for the whole app.
 *
 * <p>Use this class instead of creating new {@link ObjectMapper} instances in services, handlers,
 * or connection code. Keeping one mapper here gives the client and server the same JSON rules for
 * Java time, enums, unknown fields, and generic response payloads.
 */
public class JsonUtil {

  private static final ObjectMapper mapper = new ObjectMapper();

  // Accept the standard Jackson/ISO form ("2026-05-24T10:15:30") and the common
  // space-separated form ("2026-05-24 10:15:30") when reading LocalDateTime.
  private static final DateTimeFormatter FLEXIBLE_DATE_TIME_FORMATTER =
      new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd")
          .optionalStart()
          .appendLiteral('T')
          .optionalEnd()
          .optionalStart()
          .appendLiteral(' ')
          .optionalEnd()
          .appendPattern("HH:mm:ss")
          .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
          .optionalEnd()
          .toFormatter();

  // Runs once when JsonUtil is first used. Every method below shares this configuration.
  static {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(
        LocalDateTime.class, new LocalDateTimeDeserializer(FLEXIBLE_DATE_TIME_FORMATTER));
    mapper.registerModule(javaTimeModule);

    // Ignore extra JSON fields so older client/server code can still read newer DTOs.
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Accept enum values regardless of upper/lower case.
    mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);

    // Write LocalDateTime as readable ISO strings instead of numeric timestamps.
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  /** Converts a Java object into JSON text for socket messages or external requests. */
  public static String toJson(Object obj) throws Exception {
    return mapper.writeValueAsString(obj);
  }

  /** Converts JSON text into a normal DTO or entity class, such as LoginRequest.class. */
  public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
    return mapper.readValue(json, clazz);
  }

  /** Reads JSON as a tree when code needs to inspect envelope fields before parsing a DTO. */
  public static JsonNode readTree(String json) throws JsonProcessingException {
    return mapper.readTree(json);
  }

  /** Returns a field as text, or null when the field is missing or explicitly null. */
  public static String fieldText(JsonNode node, String fieldName) {
    JsonNode field = node.path(fieldName);
    if (field.isMissingNode() || field.isNull()) {
      return null;
    }
    return field.asText();
  }

  /** Returns a nested field as raw JSON text, or null when the field is missing or null. */
  public static String fieldJson(JsonNode node, String fieldName) {
    JsonNode field = node.path(fieldName);
    if (field.isMissingNode() || field.isNull()) {
      return null;
    }
    return field.toString();
  }

  /**
   * Parses ResponseMessage<T>.
   *
   * <p>Jackson needs this explicit type blueprint because Java does not keep enough generic type
   * information at runtime to know what T is.
   */
  public static <T> ResponseMessage<T> fromResponseJson(String json, Class<T> payloadClass)
      throws Exception {
    JavaType responseType =
        mapper.getTypeFactory().constructParametricType(ResponseMessage.class, payloadClass);
    return mapper.readValue(json, responseType);
  }

  /** Parses ResponseMessage<T> and returns only its payload. */
  public static <T> T responsePayload(String json, Class<T> payloadClass) throws Exception {
    return fromResponseJson(json, payloadClass).getPayload();
  }
}
