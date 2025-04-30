package com.eouna.scmusicgenerator.core.event;

import com.eouna.scmusicgenerator.core.FxApplicationLoader;
import com.eouna.scmusicgenerator.core.boot.context.ApplicationContext;
import com.eouna.scmusicgenerator.core.boot.env.CommandLineAndArgs;

/**
 * 程序启动失败的事件
 *
 * @author CCL
 * @date 2023/7/7
 */
public class FxApplicationFailEvent extends FxApplicationEvent {

  private final ApplicationContext context;

  /** 异常 */
  private final Throwable throwable;

  public FxApplicationFailEvent(
      FxApplicationLoader fxApplicationLoader,
      CommandLineAndArgs args,
      ApplicationContext context,
      Throwable throwable) {
    super(fxApplicationLoader, args);
    this.context = context;
    this.throwable = throwable;
  }

  public ApplicationContext getContext() {
    return context;
  }

  public Throwable getThrowable() {
    return throwable;
  }
}
