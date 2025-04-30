package com.eouna.scmusicgenerator.core.context;

import com.eouna.scmusicgenerator.core.annotaion.NoneNull;
import com.eouna.scmusicgenerator.core.boot.context.ApplicationContext;
import com.eouna.scmusicgenerator.core.event.ApplicationEvent;

/**
 * @author CCL
 * @date 2023/9/27
 */
public class PayloadApplicationEvent<T> extends ApplicationEvent {

  private final T payload;

  /**
   * Constructs a prototypical Event.
   *
   * @param source the object on which the Event initially occurred
   * @throws IllegalArgumentException if source is null
   */
  public PayloadApplicationEvent(Object source, @NoneNull T payloadObject) {
    super(source);
    this.payload = payloadObject;
  }

  public T getPayload() {
    return payload;
  }
}
