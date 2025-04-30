package com.eouna.scmusicgenerator.core.factory.support;

import com.eouna.scmusicgenerator.core.factory.IBeanFactory;
import com.eouna.scmusicgenerator.core.factory.anno.Component;
import com.eouna.scmusicgenerator.core.factory.config.BeanPostHooker;

/**
 * 自动转载bean工厂,用于加载注解了{@link Component}的bean
 *
 * @author CCL
 */
public interface AutowireBeanFactory
    extends IBeanFactory, SingletonBeanRegistry, BeanDefinitionRegistry {

  /**
   * 通过beanClass创建bean实例
   *
   * @param beanClass bean类对象
   * @return bean实例
   * @param <T> bean类型
   */
  <T> T createBean(Class<T> beanClass);

  /**
   * 销毁bean
   *
   * @param existingBean 存在的bean
   */
  void destroyBean(Object existingBean);

  /**
   * 添加bean创建时调用的钩子
   *
   * @param beanPostHooker bean处理钩子
   */
  void registerBeanPostHooker(BeanPostHooker beanPostHooker);
}
