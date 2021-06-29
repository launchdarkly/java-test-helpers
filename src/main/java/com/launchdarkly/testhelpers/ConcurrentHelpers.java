package com.launchdarkly.testhelpers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.launchdarkly.testhelpers.InternalHelpers.timeUnit;

/**
 * Helper methods and test assertions related to concurrent data structures.
 */
public abstract class ConcurrentHelpers {
  /**
   * Asserts that a future is completed within the specified timeout.
   * 
   * @param future the future
   * @param timeout the maximum time to wait
   * @param timeoutUnit the time unit for the timeout (null defaults to milliseconds)
   * @throws AssertionError if the timeout expires
   */
  public static void assertFutureIsCompleted(Future<?> future, long timeout, TimeUnit timeoutUnit) {
    try {
      future.get(timeout, timeUnit(timeoutUnit));
    } catch (TimeoutException e) {
      throw new AssertionError("timed out waiting for Future");
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Asserts that a future is completed within the specified timeout.
   * 
   * @param future the future
   * @param timeout the maximum time to wait
   * @param timeoutUnit the time unit for the timeout (null defaults to milliseconds)
   * @throws AssertionError if the future is completed
   */
  public static void assertFutureIsNotCompleted(Future<?> future, long timeout, TimeUnit timeoutUnit) {
    try {
      future.get(timeout, timeUnit(timeoutUnit));
      throw new AssertionError("Future was unexpectedly completed");
    } catch (TimeoutException e) {
      return;
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Waits for a value to be available from a {@code BlockingQueue} and consumes the value.
   * 
   * @param <T> the value type
   * @param values the queue
   * @param timeout the maximum time to wait
   * @param timeoutUnit the time unit for the timeout (null defaults to milliseconds)
   * @return the value obtained from the queue
   * @throws AssertionError if the timeout expires
   */
  public static <T> T awaitValue(BlockingQueue<T> values, long timeout, TimeUnit timeoutUnit) {
    try {
      T value = values.poll(timeout, timeUnit(timeoutUnit));
      if (value == null) {
        throw new AssertionError("did not receive expected value within " + timeout);
      }
      return value;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Asserts that no values are available fro a queue within the specified timeout.
   * 
   * @param <T> the value type
   * @param values the queue
   * @param timeout the maximum time to wait
   * @param timeoutUnit the time unit for the timeout (null defaults to milliseconds)
   * @throws AssertionError if a value was available from the queue
   */
  public static <T> void assertNoMoreValues(BlockingQueue<T> values, long timeout, TimeUnit timeoutUnit) {
    try {
      T value = values.poll(timeout, timeUnit(timeoutUnit));
      if (value != null) {
        throw new AssertionError("expected no more values, but received: " + value);
      }
    } catch (InterruptedException e) {}
  }
  
  /**
   * Shortcut for calling {@code Thread.sleep()} when an {@code InterruptedException} is not
   * expected, so you do not have to catch it.
   * 
   * @param delay the length of time to wait
   * @param delayUnit the time unit for the delay (null defaults to milliseconds)
   * @throws RuntimeException if an {@code InterruptedException} unexpectedly happened
   */
  public static void trySleep(long delay, TimeUnit delayUnit) {
    try {
      Thread.sleep(timeUnit(delayUnit).toMillis(delay));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
