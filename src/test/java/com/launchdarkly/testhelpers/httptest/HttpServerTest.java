package com.launchdarkly.testhelpers.httptest;

import org.junit.Test;

import static com.launchdarkly.testhelpers.httptest.TestUtil.simpleGet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import okhttp3.Response;

@SuppressWarnings("javadoc")
public class HttpServerTest {
  @Test
  public void serverWithSimpleStatusHandler() throws Exception {
    try (HttpServer server = HttpServer.start(Handlers.status(419))) {
      Response resp = simpleGet(server.getUri());
      assertThat(resp.code(), equalTo(419));
    }
  }
  
  @Test
  public void serverOnSpecificPort() throws Exception {
    try (HttpServer server = HttpServer.start(12345, Handlers.status(419))) {
      assertThat(server.getPort(), equalTo(12345));
      assertThat(server.getUri().toString(), equalTo("http://localhost:12345/"));
      Response resp = simpleGet(server.getUri());
      assertThat(resp.code(), equalTo(419));
    }
  }
  
  @Test
  public void multipleServers() throws Exception {
    try (HttpServer server1 = HttpServer.start(Handlers.status(200))) {
      try (HttpServer server2 = HttpServer.start(Handlers.status(419))) {
        Response resp1 = simpleGet(server1.getUri());
        Response resp2 = simpleGet(server2.getUri());
        assertThat(resp1.code(), equalTo(200));
        assertThat(resp2.code(), equalTo(419));
      }
    }
  }
}
