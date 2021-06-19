package com.launchdarkly.testhelpers.httptest;

import javax.servlet.http.HttpServletResponse;

/**
 * An object or lambda that handles HTTP requests for a {@link HttpServer}.
 * <p>
 * Use the factory methods in {@link Handlers} to create standard implementations.
 */
@FunctionalInterface
public interface Handler {
  /**
   * Processes the request.
   * 
   * @param context a {@link RequestContext} that provides both the request information
   *   and the {@link HttpServletResponse}
   */
  public void apply(RequestContext context);
}
