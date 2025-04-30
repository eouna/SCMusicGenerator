package com.eouna.scmusicgenerator.core.event;

import com.eouna.scmusicgenerator.core.FxApplicationLoader;
import com.eouna.scmusicgenerator.core.boot.context.ApplicationContext;
import com.eouna.scmusicgenerator.core.boot.env.CommandLineAndArgs;

/**
 * 程序加载完成事件 容器初始化完成且bean加载完成
 *
 * @author CCL
 * @date 2023/7/7
 */
public class FxApplicationLoadFinishEvent extends FxApplicationEvent {

  private final ApplicationContext context;

  public FxApplicationLoadFinishEvent(
      FxApplicationLoader fxApplicationLoader,
      CommandLineAndArgs args,
      ApplicationContext context) {
    super(fxApplicationLoader, args);
    this.context = context;
  }

  public ApplicationContext getContext() {
    return context;
  }
}
