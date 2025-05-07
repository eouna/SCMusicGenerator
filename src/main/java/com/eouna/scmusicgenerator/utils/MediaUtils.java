package com.eouna.scmusicgenerator.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

/**
 * 媒体工具
 *
 * @author CCL
 */
public class MediaUtils {

  public static void musicPlay(String resourcePath) {
    Media media = new Media(FileUtils.getFullResourceUrl(resourcePath));
    MediaPlayer mediaPlayer = new MediaPlayer(media);
    mediaPlayer.play();
  }
}
