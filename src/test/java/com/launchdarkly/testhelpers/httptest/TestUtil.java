package com.launchdarkly.testhelpers.httptest;

import java.net.URI;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("javadoc")
public class TestUtil {
  public static final OkHttpClient client = new OkHttpClient();
  
  public static Response simpleGet(URI uri) {
    try {
      return client.newCall(new Request.Builder().url(uri.toURL()).build()).execute();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
