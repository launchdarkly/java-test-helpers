package com.launchdarkly.testhelpers.httptest;

import javax.servlet.http.HttpServletResponse;

/**
 * An abstraction used by {@link Handler} implementations to hide the details of
 * the underlying HTTP server framework.
 */
public final class RequestContext {
  private final RequestInfo request;
  private final HttpServletResponse response;
  private final String[] pathParams;
  
  /**
   * Constructs an instance.
   * 
   * @param request the HTTP request object
   * @param response the HTTP response object
   * @param pathParams optional path parameters
   */
  public RequestContext(RequestInfo request, HttpServletResponse response, String[] pathParams) {
    this.request = request;
    this.response = response;
    this.pathParams = pathParams;
  }

  /**
   * Constructs an instance.
   * 
   * @param request the HTTP request object
   * @param response the HTTP response object
   */
  public RequestContext(RequestInfo request, HttpServletResponse response) {
    this(request, response, null);
  }

  /**
   * Returns the {@link RequestInfo}.
   * 
   * @return a {@link RequestInfo}
   */
  public RequestInfo getRequest() {
    return request;
  }
  
  /**
   * Returns the {@link HttpServletResponse}.
   * 
   * @return an {@link HttpServletResponse}
   */
  public HttpServletResponse getResponse() {
    return response;
  }
  
  /**
   * Returns a path parameter, if any path parameters were captured.
   * <p>
   * By default, this will always return null. It is non-null only if you used
   * {@link SimpleRouter} and matched a regex pattern that was added with
   * {@link SimpleRouter#addRegex(java.util.regex.Pattern, Handler)}, and the pattern
   * contained capture groups. For instance, if the pattern was {@code /a/([^/]*)/c/(.*)}
   * and the request path was {@code /a/b/c/d/e}, {@code getPathParam(0)} would return
   * {@code "b"} and {@code getPathParam(1)} would return {@code "d/e"}.
   * 
   * @param i a zero-based positional index
   * @return the path parameter string; null if there were no path parameters, or if the index
   *   is out of range
   */
  public String getPathParam(int i) {
    return pathParams != null && i >= 0 && i < pathParams.length ? pathParams[i] : null;
  }
}
