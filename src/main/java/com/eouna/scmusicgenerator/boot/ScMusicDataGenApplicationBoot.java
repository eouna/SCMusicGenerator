package com.eouna.scmusicgenerator.boot;

import com.eouna.scmusicgenerator.core.boot.context.ApplicationContext;
import com.eouna.scmusicgenerator.core.event.FxApplicationStartedEvent;
import com.eouna.scmusicgenerator.core.context.ApplicationListener;
import com.eouna.scmusicgenerator.core.window.WindowManager;
import com.eouna.scmusicgenerator.ui.controllers.ScMusicGeneratorController;
import com.eouna.scmusicgenerator.utils.FileUtils;
import com.eouna.scmusicgenerator.core.logger.LoggerUtils;
import javafx.stage.Stage;

/**
 * 程序启动器 处理容器之上的初始化逻辑
 *
 * @author CCL
 * @date 2023/7/6
 */
public class ScMusicDataGenApplicationBoot
    implements ApplicationListener<FxApplicationStartedEvent> {

  @Override
  public void onEventHappen(FxApplicationStartedEvent event) {
    ApplicationContext applicationContext = event.getApplicationContext();
    Stage stage = applicationContext.getMainStage();
    // 初始化日志系统
    LoggerUtils.getInstance().init();
    LoggerUtils.getLogger().info("初始化日志系统成功");

    // 禁止窗口重新设置大小
    stage.setResizable(false);
    stage.setMaximized(false);
    // 从pom文件中读取版本信息
    stage.setTitle("SC音乐文件生成器" + FileUtils.getAppVersion());
    // 加载主场景
    ScMusicGeneratorController scMusicGeneratorController =
        WindowManager.getInstance().openWindowWithStage(stage, ScMusicGeneratorController.class);
    // 加载主场景结束日志
    scMusicGeneratorController
        .getTextAreaLogger()
        .info("加载主场景结束,程序PID: " + FileUtils.getPid());
    // 监听主窗口关闭事件 主窗口关闭则关闭所有窗口
    stage.setOnCloseRequest((e) -> WindowManager.getInstance().destroyAllWindow());
  }
}
