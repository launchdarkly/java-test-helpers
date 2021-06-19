package com.launchdarkly.testhelpers.httptest;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An object that records all requests.
 *
 * Normally you won't need to use this class directly, because {@link HttpServer} has a
 * built-in instance that captures all requests. You can use it if you need to capture
 * only a subset of requests.
 */
public final class RequestRecorder implements Handler {
  /**
   * The default timeout for {@link #requireRequest()}: 5 seconds.
   */
  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
  
  private final BlockingQueue<RequestInfo> requests = new LinkedBlockingQueue<>();
  
  @Override
  public void apply(RequestContext context) {
    requests.add(context.getRequest());
  }

  /**
   * The number of requests currently in the queue.
   * 
   * @return the number of stored requests that have not been consumed
   */
  public int count() {
    return requests.size();
  }

  /**
   * Consumes and returns the first request in the queue, blocking until one is available,
   * using {@link #DEFAULT_TIMEOUT}.
   * 
   * @return the request information
   * @throws IllegalStateException if the timeout expires
   */
  public RequestInfo requireRequest() {
    return requireRequest(DEFAULT_TIMEOUT);
  }
  
  /**
   * Consumes and returns the first request in the queue, blocking until one is available.
   * 
   * @param timeout the maximum length of time to wait
   * @return the request information
   * @throws RuntimeException if the timeout expires
   */
  public RequestInfo requireRequest(Duration timeout) {
    try {
      RequestInfo ret = requests.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
      if (ret == null) {
        throw new IllegalStateException(new TimeoutException());
      }
      return ret;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Asserts that there are no requests in the queue and none are received within
   * the specified timeout.
   * 
   * @param timeout the maximum length of time to wait
   * @throws IllegalStateException if a request was received
   */
  public void requireNoRequests(Duration timeout) {
    try {
      RequestInfo ret = requests.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
      if (ret != null) {
         throw new IllegalStateException("received an unexpected request");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
