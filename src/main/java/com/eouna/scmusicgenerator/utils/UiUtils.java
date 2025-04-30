package com.eouna.scmusicgenerator.utils;

import com.eouna.scmusicgenerator.ScMusicDataGenApplication;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

/**
 * 界面工具
 *
 * @author CCL
 */
public class UiUtils {

  /**
   * 获取资源下的loader
   *
   * @param windowViewName 窗口资源名 路径位于ui下的相对路径名
   * @return loader
   */
  public static FXMLLoader getFxmlLoader(String windowViewName) {
    return new FXMLLoader(
        ScMusicDataGenApplication.class.getResource("ui/" + windowViewName + ".fxml"));
  }

  /**
   * 更新背景图片
   *
   * @param imageUrl 图片路径
   */
  public static void updateComponentBackgroundImg(Region component, String imageUrl) {
    Platform.runLater(
        () ->
            component.setBackground(
                new Background(
                    new BackgroundImage(
                        new Image(imageUrl),
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(
                            BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, true)))));
  }

  /**
   * 更新背景图片
   *
   * @param imageUrl 图片路径
   */
  public static void updateComponentBackgroundImg(
      Region component, String imageUrl, double width, double height) {
    Platform.runLater(
        () ->
            component.setBackground(
                new Background(
                    new BackgroundImage(
                        new Image(imageUrl),
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(width, height, false, false, true, false)))));
  }
}
