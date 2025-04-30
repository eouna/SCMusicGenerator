package com.eouna.scmusicgenerator;

import com.eouna.scmusicgenerator.boot.ScMusicDataGenApplicationBoot;
import com.eouna.scmusicgenerator.core.FxApplicationLoader;
import com.eouna.scmusicgenerator.core.annotaion.FxApplication;
import com.eouna.scmusicgenerator.core.event.FxApplicationStartedEvent;
import com.eouna.scmusicgenerator.utils.FileUtils;
import com.eouna.scmusicgenerator.utils.NodeUtils;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * 配置表生成工具,具体初始化逻辑{@link ScMusicDataGenApplicationBoot#onEventHappen(FxApplicationStartedEvent)}
 *
 * @author CCL
 */
@FxApplication(componentScanPath = {"com.eouna"})
public class ScMusicDataGenApplication extends Application {

  @Override
  public void start(Stage stage) {
    // 初始化全局stage
    FxApplicationLoader.run(ScMusicDataGenApplication.class, stage, getParameters());
    // 注册和初始系统服务
    registerSysServices();
  }


  private void registerSysServices(){
    // 设置主机服务
    NodeUtils.hostServices = ScMusicDataGenApplication.this.getHostServices();
    // 添加关闭监听事件
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook-thread"));
  }

  @Override
  public void stop() throws Exception {
    super.stop();
    // 处理关闭逻辑
    shutdown();
  }

  private void shutdown() {

  }

  public static void main(String[] args) throws Exception {
    FileUtils.genServerPid();
    ScMusicDataGenApplication.launch(args);
  }
}
