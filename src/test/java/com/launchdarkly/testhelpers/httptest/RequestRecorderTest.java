package com.launchdarkly.testhelpers.httptest;

import com.google.common.collect.ImmutableList;
import com.launchdarkly.testhelpers.BaseTest;

import org.junit.Test;

import java.net.URI;

import static com.launchdarkly.testhelpers.httptest.TestUtil.client;
import static com.launchdarkly.testhelpers.httptest.TestUtil.simpleGet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("javadoc")
public class RequestRecorderTest extends BaseTest {
  @Test
  public void requestWithoutBody() throws Exception {
    try (HttpServer server = HttpServer.start(Handlers.status(200))) {
      URI requestedUri = server.getUri().resolve("/request/path");
      Response resp = client.newCall(
          new Request.Builder().url(requestedUri.toURL())
            .header("name1", "value1")
            .addHeader("name2", "value2a")
            .addHeader("name2", "value2b")
            .build()
          ).execute();

      assertThat(resp.code(), equalTo(200));
      
      RequestInfo received = server.getRecorder().requireRequest();
      assertThat(received.getMethod(), equalTo("GET"));
      assertThat(received.getUri(), equalTo(requestedUri));
      assertThat(received.getPath(), equalTo("/request/path"));
      assertThat(received.getQuery(), nullValue());
      assertThat(received.getHeader("name1"), equalTo("value1"));
      assertThat(received.getHeader("name2"), equalTo("value2a"));
      assertThat(received.getHeaderValues("name2"), equalTo(ImmutableList.of("value2a", "value2b")));
      assertThat(ImmutableList.copyOf(received.getHeaderNames()), hasItems("name1", "name2"));
      assertThat(received.getBody(), equalTo(""));
    }
  }
  
  @Test
  public void requestWithQueryString() throws Exception {
    try (HttpServer server = HttpServer.start(Handlers.status(200))) {
      URI requestedUri = server.getUri().resolve("/request/path?a=b");
      Response resp = client.newCall(
          new Request.Builder().url(requestedUri.toURL())
            .build()
          ).execute();

      assertThat(resp.code(), equalTo(200));
      
      RequestInfo received = server.getRecorder().requireRequest();
      assertThat(received.getMethod(), equalTo("GET"));
      assertThat(received.getUri(), equalTo(requestedUri));
      assertThat(received.getPath(), equalTo("/request/path"));
      assertThat(received.getQuery(), equalTo("?a=b"));
      assertThat(received.getBody(), equalTo(""));
    }
  }
  
  @Test
  public void requestWithBody() throws Exception {
    try (HttpServer server = HttpServer.start(Handlers.status(200))) {
      URI requestedUri = server.getUri().resolve("/request/path");
      Response resp = client.newCall(
          new Request.Builder().url(requestedUri.toURL())
            .method("POST", RequestBody.create("hello", MediaType.parse("text/plain")))
            .build()
          ).execute();

      assertThat(resp.code(), equalTo(200));
      
      RequestInfo received = server.getRecorder().requireRequest();
      assertThat(received.getMethod(), equalTo("POST"));
      assertThat(received.getUri(), equalTo(requestedUri));
      assertThat(received.getPath(), equalTo("/request/path"));
      assertThat(received.getQuery(), nullValue());
      assertThat(received.getHeader("Content-Type"), startsWith("text/plain"));
      assertThat(received.getBody(), equalTo("hello"));
    }
  }
  
  @Test
  public void canDisableRecorder() throws Exception {
    try (HttpServer server = HttpServer.start(Handlers.status(200))) {
      simpleGet(server.getUri().resolve("/path1"));
      
      server.getRecorder().setEnabled(false);
      
      simpleGet(server.getUri().resolve("/path2"));
      simpleGet(server.getUri().resolve("/path3"));
      
      server.getRecorder().setEnabled(true);
      
      simpleGet(server.getUri().resolve("/path4"));
      
      RequestInfo received1 = server.getRecorder().requireRequest();
      assertThat(received1.getPath(), equalTo("/path1"));

      RequestInfo received2 = server.getRecorder().requireRequest();
      assertThat(received2.getPath(), equalTo("/path4"));
      
      assertThat(server.getRecorder().count(), equalTo(0));
    }
  }
}
