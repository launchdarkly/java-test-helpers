package com.launchdarkly.testhelpers;

import java.util.concurrent.TimeUnit;

abstract class InternalHelpers {
  public static TimeUnit timeUnit(TimeUnit unit) {
    return unit == null ? TimeUnit.MILLISECONDS : unit;
  }
}
