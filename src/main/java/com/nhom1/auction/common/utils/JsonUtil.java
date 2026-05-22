package com.nhom1.auction.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/* This class hides all the Jackson logic
To convert OBJECT TO JSON, call JsonUtil.toJson(Object obj)
To convert JSON TO OBJECT, call fromJson(String json, Class<T> clazz) - use Reflection (học roi mà k nhớ :))
 */

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    // This static block runs EXACTLY ONCE when the program starts so Jackson understands Java LocalDateTime
    static {
        mapper.registerModule(new JavaTimeModule());
        // Compatibility across client/server versions:
        // do not fail when request payload contains extra fields.
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Accept enum values regardless of upper/lower case.
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        // Keep Java time as ISO strings (not timestamps).
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    public static String toJson(Object obj) throws Exception {
        return mapper.writeValueAsString(obj);
    }

    /*
     * Converts a JSON String back into a Java Object using a "Blueprint" (Class).
     * Syntax breakdown:
     * - <T>: This is a placeholder for "Any Type".
     * - Class<T> clazz: This is the "Blueprint" (e.g., User.class or Auction.class).
     * Usage: User u = JsonUtil.fromJson(jsonText, User.class);
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return mapper.readValue(json, clazz);
    }
}
