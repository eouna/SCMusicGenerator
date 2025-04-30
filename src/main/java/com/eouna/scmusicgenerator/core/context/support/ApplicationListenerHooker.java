package com.eouna.scmusicgenerator.core.context.support;

import com.eouna.scmusicgenerator.core.context.AbstractApplicationContext;
import com.eouna.scmusicgenerator.core.context.ApplicationListener;
import com.eouna.scmusicgenerator.core.factory.config.BeanPostHooker;

/**
 * 程序监听器钩子,用于动态注册监听器
 *
 * @author CCL
 * @date 2023/9/27
 */
public class ApplicationListenerHooker implements BeanPostHooker {

  private final AbstractApplicationContext annotationApplicationContext;

  public ApplicationListenerHooker(AbstractApplicationContext annotationApplicationContext) {
    this.annotationApplicationContext = annotationApplicationContext;
  }

  @Override
  public Object postAfterInitialized(Object bean, String beanName) {
    if (bean instanceof ApplicationListener<?>) {
      this.annotationApplicationContext.addApplicationListener((ApplicationListener<?>) bean);
    }
    return bean;
  }
}
