package com.eouna.scmusicgenerator.utils;

import com.eouna.scmusicgenerator.core.logger.LoggerUtils;
import com.eouna.scmusicgenerator.core.logger.TextAreaLogger;
import com.eouna.scmusicgenerator.core.window.WindowManager;
import com.eouna.scmusicgenerator.ui.controllers.ScMusicGeneratorController;
import javafx.scene.text.TextFlow;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 主窗口日志区域
 *
 * @author CCL
 */
public class MainWindowTextAreaLogger extends TextAreaLogger {

  /** 当日志显示区域刷新后的回调 */
  protected Runnable scrollPanelFreshFunc =
      () -> {
        if (WindowManager.getInstance().isWindowInitialized(ScMusicGeneratorController.class)) {
          ScMusicGeneratorController mainWindowController =
              WindowManager.getInstance().getController(ScMusicGeneratorController.class);
          // 重新计算滚动条高度
          mainWindowController.getLogShowScrollPane().setVvalue(1D);
        }
      };

  public MainWindowTextAreaLogger(TextFlow specifyLoggerArea) {
    super(specifyLoggerArea);
  }

  @Override
  public void info(String msg, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(
            loggerShowAreaUndertaker, LoggerUtils.LogLevel.INFO, msg, scrollPanelFreshFunc, arrays);
  }

  @Override
  public void success(String msg, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(
            loggerShowAreaUndertaker,
            LoggerUtils.LogLevel.SUCCESS,
            msg,
            scrollPanelFreshFunc,
            arrays);
  }

  @Override
  public void debug(String debugMsg, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(
            loggerShowAreaUndertaker,
            LoggerUtils.LogLevel.DEBUG,
            debugMsg,
            scrollPanelFreshFunc,
            arrays);
  }

  @Override
  public void warn(String warnMsg, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(
            loggerShowAreaUndertaker,
            LoggerUtils.LogLevel.WARN,
            warnMsg,
            scrollPanelFreshFunc,
            arrays);
  }

  @Override
  public void error(String errorMsg, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(
            loggerShowAreaUndertaker,
            LoggerUtils.LogLevel.ERROR,
            errorMsg,
            scrollPanelFreshFunc,
            arrays);
  }

  @Override
  public void error(String errorMsg, Exception e, Object... arrays) {
    LoggerUtils.getInstance()
        .appendLogToTextarea(
            loggerShowAreaUndertaker,
            LoggerUtils.LogLevel.ERROR,
            errorMsg + " exception: \n" + ExceptionUtils.getStackTrace(e),
            scrollPanelFreshFunc,
            arrays);
  }
}
