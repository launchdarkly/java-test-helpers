package com.launchdarkly.testhelpers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test assertions that may be helpful in testing generic type behavior.
 *
 * @since 1.1.0
 */
public abstract class TypeBehavior {
  /**
   * A supplier interface for use {@link #checkEqualsAndHashCode(List)}.
   *
   * @param <T> the value type
   */
  public interface ValueFactory<T> {
    /**
     * Returns a new instance of the value type.
     * 
     * @return an instance
     */
    T get();
  }
  
  /**
   * Creates a simple {@link ValueFactory} that returns the specified instances in order
   * each time it is called. After all instances are used, it starts over at the first.
   * This is for use with {@link #checkEqualsAndHashCode(List)}.
   * 
   * @param <T> the value type
   * @param values the instances
   * @return a value factory
   */
  @SuppressWarnings("unchecked")
  public static <T> ValueFactory<T> valueFactoryFromInstances(T...values) {
    AtomicInteger counter = new AtomicInteger(0);
    return () -> {
      int i = counter.getAndIncrement();
      if (counter.get() >= values.length) {
        counter.set(0);
      }
      return values[i];
    };
  }
  
  /**
   * Implements a standard test suite for custom implementations of {@code equals()} and
   * {@code hashCode()}.
   * <p>
   * The {@code valueFactories} parameter is a list of value factories. Each factory must
   * produce only instances that are equal to each other, and not equal to the instances
   * produced by any of the other factories. The test suite verifies the following:
   * <ul>
   * <li> For any instance {@code a} created by any of the factories, {@code a.equals(a)}
   * is true, {@code a.equals(null)} is false, and {@code a.equals(x)} where {@code x} is
   * an instance of a different class is false. </li> 
   * <li> For any two instances {@code a} and {@code b} created by the same factory,
   * {@code a.equals(b)}, {@code b.equals(a)}, and {@code a.hashCode() == b.hashCode()}
   * are all true. </li>
   * <li> For any two instances {@code a} and {@code b} created by different factories,
   * {@code a.equals(b)} and {@code b.equals(a)} are false (there is no requirement that
   * the hash codes are different). </li>
   * </ul>
   * 
   * @param <T> the value type
   * @param valueFactories list of factories for distinct values
   * @throws AssertionError if a test condition fails
   */
  public static <T> void checkEqualsAndHashCode(List<ValueFactory<T>> valueFactories) {
    for (int i = 0; i < valueFactories.size(); i++) {
      for (int j = 0; j < valueFactories.size(); j++) {
        T value1 = valueFactories.get(i).get();
        T value2 = valueFactories.get(j).get();
        if (value1 == value2) {
          throw new AssertionError("value factory must not return the same instance twice");
        }
        if (i == j) {
          // instance is equal to itself
          if (!value1.equals(value1)) {
            throw new AssertionError("value was not equal to itself: " + value1);
          }
 
          // commutative equality
          if (!value1.equals(value2)) {
            throw new AssertionError("(" + value1 + ").equals(" + value2 + ") was false");
          }
          if (!value2.equals(value1)) {
            throw new AssertionError("(" + value1 + ").equals(" + value2 + ") was true, but (" +
                value2 + ").equals(" + value1 + ") was false");
          }
 
          // equal hash code
          if (value1.hashCode() != value2.hashCode()) {
            throw new AssertionError("(" + value1 + ").hashCode() was " + value1.hashCode() + " but ("
                + value2 + ").hashCode() was " + value2.hashCode());
          }
 
          // unequal to null, unequal to value of wrong class
          if (value1.equals(null)) {
            throw new AssertionError("value was equal to null: " + value1);
          }
          if (value1.equals(new Object())) {
            throw new AssertionError("value was equal to Object: " + value1);
          }
        } else {
          // commutative inequality
          if (value1.equals(value2)) {
            throw new AssertionError("(" + value1 + ").equals(" + value2 + ") was true");
          }
          if (value2.equals(value1)) {
            throw new AssertionError("(" + value2 + ").equals(" + value1 + ") was true");
          }
        }
      }
    }
  }
}
