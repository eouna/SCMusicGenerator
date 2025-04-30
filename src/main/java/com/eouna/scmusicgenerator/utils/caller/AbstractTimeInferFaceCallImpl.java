package com.eouna.scmusicgenerator.utils.caller;

import com.eouna.scmusicgenerator.core.watcher.TimeConsumeWatcher;

/**
 * @author ccl
 * @date Created in 2023/3/14
 */
public abstract class AbstractTimeInferFaceCallImpl<T> implements IInterfaceCallEachAction<T> {
  TimeConsumeWatcher timeConsumeWatcher;

  public AbstractTimeInferFaceCallImpl() {}

  public void setTimeConsumeWatcher(TimeConsumeWatcher timeConsumeWatcher) {
    this.timeConsumeWatcher = timeConsumeWatcher;
  }

  @Override
  public void callBefore(T classEntity) {
    timeConsumeWatcher.start("class#" + classEntity.getClass().getSimpleName());
    IInterfaceCallEachAction.super.callBefore(classEntity);
  }

  @Override
  public void afterCall(T classEntity) {
    timeConsumeWatcher.stop();
  }
}
