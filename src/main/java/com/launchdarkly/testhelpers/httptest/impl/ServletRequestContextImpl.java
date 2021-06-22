package com.launchdarkly.testhelpers.httptest.impl;

import com.google.common.collect.ImmutableList;
import com.launchdarkly.testhelpers.httptest.RequestContext;
import com.launchdarkly.testhelpers.httptest.RequestInfo;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Implementation of {@link RequestContext} using the servlet API.
 */
public final class ServletRequestContextImpl implements RequestContext {
  private final RequestInfo request;
  private final HttpServletResponse response;
  private final ImmutableList<String> pathParams;
  private boolean chunked = false;
  
  private ServletRequestContextImpl(RequestInfo request, HttpServletResponse response, ImmutableList<String> pathParams) {
    this.request = request;
    this.response = response;
    this.pathParams = pathParams == null ? ImmutableList.of() : pathParams;
  }

  /**
   * Constructs an instance.
   * 
   * @param request the HTTP request object
   * @param response the HTTP response object
   */
  public ServletRequestContextImpl(RequestInfo request, HttpServletResponse response) {
    this(request, response, null);
  }

  public RequestInfo getRequest() {
    return request;
  }
  
  public void setStatus(int status) {
    response.setStatus(status);
  }
  
  public void setHeader(String name, String value) {
    response.setHeader(name, value);
  }
  
  public void addHeader(String name, String value) {
    response.addHeader(name, value);
  }
  
  public void setChunked() {
    if (chunked) {
      return;
    }
    chunked = true;
    response.setHeader("Transfer-Encoding", "chunked");
  }

  public void write(byte[] data) {
    try {
      if (data != null && data.length != 0) {
        response.getOutputStream().write(data);
      }
      if (chunked) {
        response.flushBuffer();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String getPathParam(int i) {
    return i >= 0 && i < pathParams.size() ? pathParams.get(i) : null;
  }
  
  public RequestContext withPathParams(Iterable<String> pathParams) {
    return new ServletRequestContextImpl(request, response,
        pathParams == null ? null : ImmutableList.copyOf(pathParams));
  }
}
