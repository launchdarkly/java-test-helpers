package com.launchdarkly.testhelpers;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test assertions related to JSON.
 * 
 * @since 1.1.0
 */
public abstract class JsonAssertions {
  private static final Gson gson = new Gson();
  
  /**
   * Parses two strings as JSON and compares them for deep equality. If they are unequal,
   * it tries to describe the difference as specifically as possible by recursing into
   * object properties or array elements.
   * 
   * @param expected the expected JSON string
   * @param actual the actual JSON string
   * @throws AssertionError if the values are not deeply equal, or are not valid JSON
   */
  public static void assertJsonEquals(String expected, String actual) {
    JsonElement expectedJson, actualJson;
    try {
      expectedJson = gson.fromJson(expected, JsonElement.class);
    } catch (Exception e) {
      throw new AssertionError("expected string is not valid JSON: " + e);
    }
    try {
      actualJson = gson.fromJson(actual, JsonElement.class);
    } catch (Exception e) {
      throw new AssertionError("actual string is not valid JSON: " + e);
    }
    if (actualJson.equals(expectedJson)) {
      return;
    }
    String diff = describeJsonDifference(expectedJson, actualJson, "", false);
    if (diff == null) {
      diff = "expected: " + expected + "\nactual: " + actual;
    } else {
      diff = diff + "\nfull actual JSON string: " + actual;
    }
    throw new AssertionError("JSON strings did not match\n" + diff);
  }
  
  /**
   * Same as {@link #assertJsonEquals(String, String)} except that it allows any JSON
   * objects in the actual data to contain extra properties that are not in the expected
   * data.
   * 
   * @param expectedSubset the expected JSON string
   * @param actual the actual JSON string
   * @throws AssertionError if the expected values are not a subset of the actual
   *   values, or if the strings are not valid JSON
   */
  public static void assertJsonSubset(String expectedSubset, String actual) {
    JsonElement expectedJson, actualJson;
    try {
      expectedJson = gson.fromJson(expectedSubset, JsonElement.class);
    } catch (Exception e) {
      throw new AssertionError("expected string is not valid JSON: " + e);
    }
    try {
      actualJson = gson.fromJson(actual, JsonElement.class);
    } catch (Exception e) {
      throw new AssertionError("actual string is not valid JSON: " + e);
    }
    if (isJsonSubset(expectedJson, actualJson)) {
      return;
    }
    String diff = describeJsonDifference(expectedJson, actualJson, "", true);
    if (diff == null) {
      diff = "expected: " + expectedSubset + "\nactual: " + actual;
    } else {
      diff = diff + "\nfull actual JSON string: " + actual;
    }
    throw new AssertionError("JSON string did not contain expected properties\n" + diff);
  }
  
  private static boolean isJsonSubset(JsonElement expected, JsonElement actual) {
    if (expected instanceof JsonObject && actual instanceof JsonObject) {
      JsonObject eo = (JsonObject)expected, ao = (JsonObject)actual;
      for (Map.Entry<String, JsonElement> e: eo.entrySet()) {
        if (!ao.has(e.getKey()) || !isJsonSubset(e.getValue(), ao.get(e.getKey()))) {
          return false;
        }
      }
      return true;
    }
    if (expected instanceof JsonArray && actual instanceof JsonArray) {
      JsonArray ea = (JsonArray)expected, aa = (JsonArray)actual;
      if (ea.size() != aa.size()) {
        return false;
      }
      for (int i = 0; i < ea.size(); i++) {
        if (!isJsonSubset(ea.get(i), aa.get(i))) {
          return false;
        }
      }
      return true;
    }
    return actual.equals(expected);
  }
  
  private static String describeJsonDifference(
      JsonElement expected,
      JsonElement actual,
      String prefix,
      boolean allowExtraProps
      ) {
    if (actual instanceof JsonObject && expected instanceof JsonObject) {
      return describeJsonObjectDifference((JsonObject)expected, (JsonObject)actual, prefix, allowExtraProps);
    }
    if (actual instanceof JsonArray && expected instanceof JsonArray) {
      return describeJsonArrayDifference((JsonArray)expected, (JsonArray)actual, prefix, allowExtraProps);
    }
    return null;
  }

  private static String describeJsonObjectDifference(
      JsonObject expected,
      JsonObject actual,
      String prefix,
      boolean allowExtraProps
      ) {
    List<String> diffs = new ArrayList<>();
    Set<String> allKeys = new HashSet<>();
    for (Map.Entry<String, JsonElement> e: expected.entrySet()) {
      allKeys.add(e.getKey());
    }
    for (Map.Entry<String, JsonElement> e: actual.entrySet()) {
      allKeys.add(e.getKey());
    }
    for (String key: allKeys) {
      String prefixedKey = prefix + (prefix == "" ? "" : ".") + key;
      String expectedDesc = null, actualDesc = null, detailDiff = null;
      if (expected.has(key)) {
        if (actual.has(key)) {
          JsonElement actualValue = actual.get(key), expectedValue = expected.get(key);
          if (!actualValue.equals(expectedValue)) {
            expectedDesc = expectedValue.toString();
            actualDesc = actualValue.toString();
            detailDiff = describeJsonDifference(expectedValue, actualValue, prefixedKey, allowExtraProps);
          }
        } else {
          expectedDesc = expected.get(key).toString();
          actualDesc = "<absent>";
        }
      } else if (!allowExtraProps) {
        actualDesc = actual.get(key).toString();
        expectedDesc = "<absent>";
      }
      if (expectedDesc != null || actualDesc != null) {
        if (detailDiff != null) {
          diffs.add(detailDiff);
        } else {
          diffs.add(String.format("at \"%s\": expected = %s, actual = %s", prefixedKey,
              expectedDesc, actualDesc));
        }
      }
    }
    return Joiner.on("\n").join(diffs);
  }

  private static String describeJsonArrayDifference(
      JsonArray expected,
      JsonArray actual,
      String prefix,
      boolean allowExtraProps
      ) {
    if (expected.size() != actual.size()) {
      return null; // can't provide a detailed diff, just show the whole values
    }
    List<String> diffs = new ArrayList<>();
    for (int i = 0; i < expected.size(); i++) {
      String prefixedIndex = String.format("%s[%d]", prefix, i);
      JsonElement actualValue = actual.get(i), expectedValue = expected.get(i);
      if (!actualValue.equals(expectedValue)) {
        String detailDiff = describeJsonDifference(expectedValue, actualValue, prefixedIndex, allowExtraProps);
        if (detailDiff != null) {
          diffs.add(detailDiff);
        } else {
          diffs.add(String.format("at \"%s\": expected = %s, actual = %s", prefixedIndex,
              expectedValue.toString(), actualValue.toString()));
        }
      }
    }
    return Joiner.on("\n").join(diffs);
  }
}
