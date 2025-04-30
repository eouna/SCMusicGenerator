package com.eouna.scmusicgenerator.core.logger;

import com.eouna.scmusicgenerator.core.window.IWindowLogger;
import com.eouna.scmusicgenerator.core.logger.LoggerUtils.LogLevel;
import javafx.scene.text.TextFlow;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 日志文本框logger
 *
 * @author CCL
 * @date 2023/4/6
 */
public class TextAreaLogger implements IWindowLogger {

  /** 日志显示区域 */
  protected TextFlow loggerShowAreaUndertaker;

  public TextAreaLogger(TextFlow specifyLoggerArea) {
    this.loggerShowAreaUndertaker = specifyLoggerArea;
  }

  public TextFlow getLoggerShowAreaUndertaker() {
    return loggerShowAreaUndertaker;
  }

  public void setLoggerShowAreaUndertaker(TextFlow loggerShowAreaUndertaker) {
    this.loggerShowAreaUndertaker = loggerShowAreaUndertaker;
  }

  @Override
  public void info(String msg, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(loggerShowAreaUndertaker, LogLevel.INFO, msg, arrays);
  }

  @Override
  public void success(String msg, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(loggerShowAreaUndertaker, LogLevel.SUCCESS, msg, arrays);
  }

  @Override
  public void debug(String debugMsg, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(loggerShowAreaUndertaker, LogLevel.DEBUG, debugMsg);
  }

  @Override
  public void warn(String warnMsg, Object... arrays) {
    LoggerUtils.getInstance().appendLogToTextarea(loggerShowAreaUndertaker, LogLevel.WARN, warnMsg);
  }

  @Override
  public void error(String errorMsg, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(loggerShowAreaUndertaker, LogLevel.ERROR, errorMsg, arrays);
  }

  @Override
  public void error(String errorMsg, Exception e, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(
            loggerShowAreaUndertaker,
            LogLevel.ERROR,
            errorMsg + " exception: \n" + ExceptionUtils.getStackTrace(e),
            arrays);
  }
}
