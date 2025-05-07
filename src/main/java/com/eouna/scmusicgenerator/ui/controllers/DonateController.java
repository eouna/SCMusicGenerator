package com.eouna.scmusicgenerator.ui.controllers;

import com.eouna.scmusicgenerator.core.window.BaseWindowController;
import com.eouna.scmusicgenerator.utils.EffectUtils;
import com.eouna.scmusicgenerator.utils.FileUtils;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author CCL
 */
public class DonateController extends BaseWindowController {

  @FXML StackPane root;
  @FXML HBox container;
  @FXML VBox imgShow1;
  @FXML VBox imgShow2;
  @FXML VBox wechatShow;
  @FXML VBox aliShow;

  AnimationTimer animationTimer;

  @Override
  public void onCreate(Stage stage) {
    stage.setResizable(true);

    stage.widthProperty().addListener((observable, oldValue, newValue) -> stage.setWidth(810));
    stage.heightProperty().addListener((observable, oldValue, newValue) -> stage.setHeight(439));
  }

  @Override
  public void onMounted(Object... args) {
    int parentHeight = (int) container.getPrefHeight();
    int multiple = (int) Math.floor(parentHeight / 50.0);
    for (int i = 0; i < multiple; i++) {
      ImageView imageView1 = new ImageView(new Image(FileUtils.getFullResourceUrl("img/rich.gif")));
      imageView1.setFitWidth(50);
      imageView1.setFitHeight(50);
      imgShow1.getChildren().add(imageView1);
      ImageView imageView2 = new ImageView(new Image(FileUtils.getFullResourceUrl("img/rich.gif")));
      imageView2.setFitWidth(50);
      imageView2.setFitHeight(50);
      imgShow2.getChildren().add(imageView2);
    }

    ImageView wechatView =
        new ImageView(new Image(FileUtils.getFullResourceUrl("img/wechat_pay.jpg")));
    wechatView.setFitWidth(295);
    wechatView.setFitHeight(295);
    wechatShow.getChildren().add(wechatView);

    ImageView alipayView = new ImageView(new Image(FileUtils.getFullResourceUrl("img/alipay.jpg")));
    alipayView.setFitWidth(295);
    alipayView.setFitHeight(295);
    aliShow.getChildren().add(alipayView);

    EffectUtils.SimpleFireworkEffect fireworkEffect =
        new EffectUtils.SimpleFireworkEffect(root, 60, true, 1);
    animationTimer = fireworkEffect.playFireworks(root);
    animationTimer.start();

    super.onMounted(args);
  }

  @Override
  protected void onClose() {
    super.onClose();
    animationTimer.stop();
  }

  @Override
  public String getFxmlPath() {
    return "donate";
  }

  @Override
  public String getTitle() {
    return "donate";
  }

  @Override
  public String getStageIconPath() {
    return "icon/Donate.png";
  }
}
