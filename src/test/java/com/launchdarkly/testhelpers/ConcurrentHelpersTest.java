package com.launchdarkly.testhelpers;

import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.launchdarkly.testhelpers.ConcurrentHelpers.assertNoMoreValues;
import static com.launchdarkly.testhelpers.ConcurrentHelpers.awaitValue;
import static com.launchdarkly.testhelpers.ConcurrentHelpers.trySleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("javadoc")
public class ConcurrentHelpersTest {
  @Test
  public void awaitValueSuccess() {
    BlockingQueue<String> q = new LinkedBlockingQueue<>();
    new Thread(() -> {
      q.add("a");
    }).start();
    String value = awaitValue(q, 1, TimeUnit.SECONDS);
    assertThat(value, equalTo("a"));
  }
  
  @Test(expected=AssertionError.class)
  public void awaitValueFailure() {
    BlockingQueue<String> q = new LinkedBlockingQueue<>();
    awaitValue(q, 100, TimeUnit.MILLISECONDS);
  }

  @Test
  public void assertNoMoreValuesSuccess() {
    BlockingQueue<String> q = new LinkedBlockingQueue<>();
    assertNoMoreValues(q, 50, TimeUnit.MILLISECONDS);
  }

  @Test(expected=AssertionError.class)
  public void assertNoMoreValuesFailure() {
    BlockingQueue<String> q = new LinkedBlockingQueue<>();
    new Thread(() -> {
      trySleep(10, TimeUnit.MILLISECONDS);
      q.add("a");
    }).start();
    assertNoMoreValues(q, 100, TimeUnit.MILLISECONDS);
  }
}
