package com.eouna.scmusicgenerator.core.context;

import com.eouna.scmusicgenerator.core.factory.Aware;
/**
 * 程序事件推送
 *
 * @author ccl
 */
public interface ApplicationEventPublisherAware extends Aware {

  /**
   * 设置程序事件源
   *
   * @param eventDispatcher 事件生产者
   */
  void setApplicationEventPublisher(ApplicationEventPublisher eventDispatcher);
}
