package com.eouna.scmusicgenerator.core.event;

import com.eouna.scmusicgenerator.core.FxApplicationLoader;
import com.eouna.scmusicgenerator.core.boot.context.ApplicationContext;
import com.eouna.scmusicgenerator.core.boot.env.CommandLineAndArgs;

/**
 * 程序上下文加载之前的事件
 *
 * @author CCL
 * @date 2023/7/6
 */
public class FxApplicationContextLoadBeforeEvent extends FxApplicationEvent {

  private final ApplicationContext context;

  public FxApplicationContextLoadBeforeEvent(
      FxApplicationLoader fxApplicationLoader,
      CommandLineAndArgs args,
      ApplicationContext context) {
    super(fxApplicationLoader, args);
    this.context = context;
  }

  public ApplicationContext getApplicationContext() {
    return context;
  }
}
