package com.eouna.scmusicgenerator.core.context;

import com.eouna.scmusicgenerator.core.boot.env.IApplicationEnvironment;
import com.eouna.scmusicgenerator.core.factory.support.BeanDefinitionRegistry;

/**
 * bean定义适配器
 *
 * @author CCL
 * @date 2023/9/22
 */
public class AnnotationBeanDefinitionReader {

  private final BeanDefinitionRegistry beanDefinitionRegistry;

  private final IApplicationEnvironment environment;

  public AnnotationBeanDefinitionReader(
      BeanDefinitionRegistry beanDefinitionRegistry, IApplicationEnvironment environment) {
    this.beanDefinitionRegistry = beanDefinitionRegistry;
    this.environment = environment;
  }
}
