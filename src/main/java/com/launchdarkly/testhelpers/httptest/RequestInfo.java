package com.launchdarkly.testhelpers.httptest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;

import java.io.BufferedReader;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

/**
 * Properties of a request received by {@link HttpServer}.
 * <p>
 * This is used instead of the raw {@link HttpServletRequest} because {@link HttpServer} may need
 * to record the request. Therefore the request body, if any, is read in full ahead of time and
 * stored instead of providing the request input stream. 
 */
public final class RequestInfo {
  private final String method;
  private final URI uri;
  private final String path;
  private final String query;
  private final ImmutableMap<String, ImmutableList<String>> headers;
  private final String body;
  
  /**
   * Constructs an instance from an {@link HttpServletRequest}.
   * 
   * @param request the original request
   */
  public RequestInfo(HttpServletRequest request) {
    this.method = request.getMethod().toUpperCase();
    this.path = request.getPathInfo();
    this.query = request.getQueryString() == null ? null :
      ("?" + request.getQueryString());

    ImmutableMap.Builder<String, ImmutableList<String>> headers = ImmutableMap.builder();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String name = headerNames.nextElement();
      headers.put(name.toLowerCase(),
          ImmutableList.copyOf(Collections.list(request.getHeaders(name))));
    }
    this.headers = headers.build();
    
    StringBuffer url = request.getRequestURL();
    if (this.query != null) {
      url.append(this.query);
    }
    this.uri = URI.create(url.toString());
    
    try {
      BufferedReader r = request.getReader();
      if (r == null) {
        this.body = null;
      } else {
        this.body = CharStreams.toString(r);
        r.close();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the HTTP method.
   * 
   * @return the HTTP method
   */
  public String getMethod() {
    return method;
  }

  /**
   * Returns the full request URI.
   * 
   * @return the request URI
   */
  public URI getUri() {
    return uri;
  }
  
  /**
   * Returns the request path.
   * 
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Returns the request query string.
   * 
   * @return the query string (including the leading "?"), or null if there is none
   */
  public String getQuery() {
    return query;
  }
  
  /**
   * Returns a request header by name.
   * <p>
   * If the header has multiple values, it returns the first value.
   * 
   * @param name a case-insensitive header name
   * @return the header value, or null if not found
   */
  public String getHeader(String name) {
    ImmutableList<String> values = headers.get(name.toLowerCase());
    return (values == null || values.isEmpty()) ? null : values.get(0); 
  }

  /**
   * Returns all request header names.
   * 
   * @return the header names
   */
  public Iterable<String> getHeaderNames() {
    return headers.keySet();
  }
  
  /**
   * Returns all values of a request header by name.
   * 
   * @param name a case-insensitive header name
   * @return the header values, or an empty iterable if not found
   */
  public Iterable<String> getHeaderValues(String name) {
    ImmutableList<String> values = headers.get(name.toLowerCase());
    return values == null ? ImmutableList.of() : values; 
  }
  
  /**
   * Returns the request body as a string.
   * 
   * @return the request body, or null if there is none
   */
  public String getBody() {
    return body;
  }
}
