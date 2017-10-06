/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 *
 *
 * @since 4.0
 */
public class SmartLifecycleInterceptor implements LifecycleInterceptor {

  private static final Boolean DUMMY = Boolean.TRUE;
  private final Map<Object, Boolean> initialized = new WeakHashMap<>();
  private final Map<Object, Boolean> started = new WeakHashMap<>();

  @Override
  public boolean beforePhaseExecution(LifecyclePhase phase, Object object) {
    switch (phase.getName()) {
      case Initialisable.PHASE_NAME: {
        initialized.put(object, DUMMY);
        return true;
      }
      case Startable.PHASE_NAME: {
        started.put(object, DUMMY);
        return true;
      }
      case Stoppable.PHASE_NAME: {
        return started.containsKey(object);
      }
      case Disposable.PHASE_NAME: {
        return initialized.containsKey(object);
      }
      default:
        return true;
    }
  }

  @Override
  public void afterPhaseExecution(LifecyclePhase phase, Object object, Optional<Exception> exceptionThrownOptional) {

  }

  @Override
  public void onPhaseCompleted(LifecyclePhase phase) {

  }

}
