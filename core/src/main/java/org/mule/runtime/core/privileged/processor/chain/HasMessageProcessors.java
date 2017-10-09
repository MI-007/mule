package org.mule.runtime.core.privileged.processor.chain;

import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

public interface HasMessageProcessors {

  List<Processor> getMessageProcessors();
}
