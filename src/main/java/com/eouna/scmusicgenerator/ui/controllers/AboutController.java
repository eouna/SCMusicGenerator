package com.eouna.scmusicgenerator.ui.controllers;

import com.eouna.scmusicgenerator.constant.DefaultEnvConfigConstant;
import com.eouna.scmusicgenerator.core.window.BaseWindowController;
import com.eouna.scmusicgenerator.utils.FileUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.Calendar;

/**
 * @author CCL
 */
public class AboutController extends BaseWindowController {
  @FXML protected Label versionId;
  @FXML protected Label authorName;

  @FXML protected Label powerName;
  @FXML protected Label copyRightName;

  @Override
  public void onCreate(Stage stage) {
    stage.setResizable(false);
  }

  @Override
  public void onMounted(Object... args) {
    super.onMounted(args);
    this.versionId.setText(FileUtils.getAppVersion());
    this.authorName.setText(DefaultEnvConfigConstant.AUTHOR);
    this.powerName.setText("Powered By " + DefaultEnvConfigConstant.COM_NAME + " Company");
    this.copyRightName.setText(
        "Copyright © 2018–"
            + Calendar.getInstance().get(Calendar.YEAR)
            + " "
            + DefaultEnvConfigConstant.COM_NAME);
  }

  @Override
  public String getFxmlPath() {
    return "about";
  }

  @Override
  public String getTitle() {
    return "关于";
  }

  @Override
  public String getStageIconPath() {
    return "icon/about.png";
  }
}
