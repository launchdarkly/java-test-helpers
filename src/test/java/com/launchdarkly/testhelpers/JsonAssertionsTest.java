package com.launchdarkly.testhelpers;

import org.junit.Test;

import static com.launchdarkly.testhelpers.JsonAssertions.assertJsonEquals;
import static com.launchdarkly.testhelpers.JsonAssertions.assertJsonSubset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class JsonAssertionsTest {
  @Test
  public void assertJsonEqualsSuccess() {
    assertJsonEquals("null", "null");
    assertJsonEquals("true", "true");
    assertJsonEquals("1", "1");
    assertJsonEquals("\"x\"", "\"x\"");
    assertJsonEquals("{\"a\":1,\"b\":{\"c\":2}}", "{\"b\":{\"c\":2},\"a\":1}");
    assertJsonEquals("[1,2,[3,4]]","[1,2,[3,4]]");
  }
  
  @Test
  public void assertJsonEqualsFailureWithNoDetailedDiff() {
    assertThat(jsonEqualsFailureMessage("null", "true"),
        equalTo("expected: null\nactual: true"));
    assertThat(jsonEqualsFailureMessage("false", "true"),
        equalTo("expected: false\nactual: true"));
    assertThat(jsonEqualsFailureMessage("{\"a\":1}", "3"),
        equalTo("expected: {\"a\":1}\nactual: 3"));
    assertThat(jsonEqualsFailureMessage("[1,2]", "3"),
        equalTo("expected: [1,2]\nactual: 3"));
    assertThat(jsonEqualsFailureMessage("[1,2]", "[1,2,3]"),
        equalTo("expected: [1,2]\nactual: [1,2,3]"));
  }

  @Test
  public void assertJsonEqualsFailureWithDetailedDiff() {
    assertThat(jsonEqualsFailureMessage("{\"a\":1,\"b\":2}", "{\"a\":1,\"b\":3}"),
        equalTo("at \"b\": expected = 2, actual = 3"));

    assertThat(jsonEqualsFailureMessage("{\"a\":1,\"b\":2}", "{\"a\":1}"),
        equalTo("at \"b\": expected = 2, actual = <absent>"));

    assertThat(jsonEqualsFailureMessage("{\"a\":1}", "{\"a\":1,\"b\":2}"),
        equalTo("at \"b\": expected = <absent>, actual = 2"));

    assertThat(jsonEqualsFailureMessage("{\"a\":1,\"b\":{\"c\":2}}", "{\"a\":1,\"b\":{\"c\":3}}"),
        equalTo("at \"b.c\": expected = 2, actual = 3"));

    assertThat(jsonEqualsFailureMessage("{\"a\":1,\"b\":[2,3]}", "{\"a\":1,\"b\":[3,3]}"),
        equalTo("at \"b[0]\": expected = 2, actual = 3"));

    assertThat(jsonEqualsFailureMessage("[100,200,300]", "[100,201,300]"),
        equalTo("at \"[1]\": expected = 200, actual = 201"));

    assertThat(jsonEqualsFailureMessage("[100,[200,210],300]", "[100,[201,210],300]"),
        equalTo("at \"[1][0]\": expected = 200, actual = 201"));

    assertThat(jsonEqualsFailureMessage("[100,{\"a\":1},300]", "[100,{\"a\":2},300]"),
        equalTo("at \"[1].a\": expected = 1, actual = 2"));
  }
  
  @Test
  public void assertJsonSubsetSuccess() {
   assertJsonSubset("{\"a\":1,\"b\":2}", "{\"b\":2,\"a\":1}");
   assertJsonSubset("{\"a\":1,\"b\":2}", "{\"b\":2,\"a\":1,\"c\":3}");
   assertJsonSubset("{\"a\":1,\"b\":{\"c\":2}}", "{\"b\":{\"c\":2,\"d\":3},\"a\":1}");
  }
  
  @Test
  public void assertJsonSubsetFailure() {
    assertThat(jsonSubsetFailureMessage("{\"a\":1}", "{\"a\":0,\"b\":2,\"c\":3}"),
        equalTo("at \"a\": expected = 1, actual = 0"));

    assertThat(jsonSubsetFailureMessage("{\"a\":1}", "{\"b\":2,\"c\":3}"),
        equalTo("at \"a\": expected = 1, actual = <absent>"));

    assertThat(jsonSubsetFailureMessage("{\"b\":2,\"a\":1,\"c\":3}", "{\"a\":1,\"b\":2}"),
        equalTo("at \"c\": expected = 3, actual = <absent>"));

    assertThat(jsonSubsetFailureMessage("{\"b\":{\"c\":2,\"d\":3},\"a\":1}", "{\"a\":1,\"b\":{\"c\":2}}"),
        equalTo("at \"b.d\": expected = 3, actual = <absent>"));
  }
  
  private static String jsonEqualsFailureMessage(String expected, String actual) {
    try {
      assertJsonEquals(expected, actual);
      fail("expected AssertionFailedException");
      return null;
    } catch (AssertionError e) {
      String m = e.getMessage();
      String expectedPrefix = "JSON strings did not match\n";
      assertThat(m, startsWith(expectedPrefix));
      return m.substring(expectedPrefix.length()).replaceFirst("\nfull actual.*", "");
    }
  }

  private static String jsonSubsetFailureMessage(String expected, String actual) {
    try {
      assertJsonSubset(expected, actual);
      fail("expected AssertionFailedException");
      return null;
    } catch (AssertionError e) {
      String m = e.getMessage();
      String expectedPrefix = "JSON string did not contain expected properties\n";
      assertThat(m, startsWith(expectedPrefix));
      return m.substring(expectedPrefix.length()).replaceFirst("\nfull actual.*", "");
    }
  }
}
