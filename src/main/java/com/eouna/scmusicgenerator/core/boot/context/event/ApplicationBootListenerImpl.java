package com.eouna.scmusicgenerator.core.boot.context.event;

import com.eouna.scmusicgenerator.core.FxApplicationLoader;
import com.eouna.scmusicgenerator.core.boot.IApplicationBootListener;
import com.eouna.scmusicgenerator.core.boot.context.ApplicationContext;
import com.eouna.scmusicgenerator.core.boot.env.CommandLineAndArgs;
import com.eouna.scmusicgenerator.core.event.FxApplicationContextLoadAfterEvent;
import com.eouna.scmusicgenerator.core.event.FxApplicationContextLoadBeforeEvent;
import com.eouna.scmusicgenerator.core.event.FxApplicationFailEvent;
import com.eouna.scmusicgenerator.core.event.FxApplicationLoadFinishEvent;
import com.eouna.scmusicgenerator.core.event.FxApplicationStartedEvent;
import com.eouna.scmusicgenerator.core.event.FxApplicationStartingEvent;

/**
 * 程序启动 各个阶段 监听器
 *
 * @author CCL
 * @date 2023/6/30
 */
public class ApplicationBootListenerImpl implements IApplicationBootListener {

  /** 容器加载器引用 */
  private final FxApplicationLoader fxApplicationLoader;

  /** 程序启动参数 */
  private final CommandLineAndArgs args;

  private final ApplicationEventDispatcher eventDispatcher;

  /**
   * 在 此处调用 使用反射进行调用
   *
   * @param fxApplicationLoader 程序启动器
   * @param args 启动参数
   */
  public ApplicationBootListenerImpl(FxApplicationLoader fxApplicationLoader, CommandLineAndArgs args) {
    this.fxApplicationLoader = fxApplicationLoader;
    this.args = args;
    eventDispatcher = new ApplicationEventDispatcher();
    // 注册fx程序启动时需要的监听器
    fxApplicationLoader.getListeners().forEach(eventDispatcher::registerListeners);
  }

  @Override
  public void staring() {
    this.eventDispatcher.dispatchEvent(new FxApplicationStartingEvent(fxApplicationLoader, args));
  }

  @Override
  public void contextLoadBefore(ApplicationContext context) {
    this.eventDispatcher.dispatchEvent(
        new FxApplicationContextLoadBeforeEvent(fxApplicationLoader, args, context));
  }

  @Override
  public void contextLoadAfter(ApplicationContext context) {
    this.eventDispatcher.dispatchEvent(
        new FxApplicationContextLoadAfterEvent(fxApplicationLoader, args, context));
  }

  @Override
  public void started(ApplicationContext context) {
    this.eventDispatcher.dispatchEvent(
        new FxApplicationStartedEvent(fxApplicationLoader, args, context));
  }

  @Override
  public void running(ApplicationContext context) {
    this.eventDispatcher.dispatchEvent(
        new FxApplicationLoadFinishEvent(fxApplicationLoader, args, context));
  }

  @Override
  public void failed(ApplicationContext context, Throwable throwable) {
    this.eventDispatcher.dispatchEvent(
        new FxApplicationFailEvent(fxApplicationLoader, args, context, throwable));
  }
}
