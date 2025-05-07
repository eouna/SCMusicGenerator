package com.eouna.scmusicgenerator.ui.controllers;

import com.eouna.scmusicgenerator.constant.DefaultEnvConfigConstant;
import com.eouna.scmusicgenerator.core.logger.LoggerUtils;
import com.eouna.scmusicgenerator.core.window.BaseWindowController;
import com.eouna.scmusicgenerator.core.window.MainWindowIdentifier;
import com.eouna.scmusicgenerator.core.window.WindowManager;
import com.eouna.scmusicgenerator.generator.midi.MidiFileDecoder;
import com.eouna.scmusicgenerator.generator.midi.ScSoundDataPanelRender;
import com.eouna.scmusicgenerator.generator.mp3.Mp3ToMidiDecoder;
import com.eouna.scmusicgenerator.utils.*;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * excel生成展示窗口
 *
 * @author CCL
 */
@MainWindowIdentifier
public class ScMusicGeneratorController extends BaseWindowController {

  // region============================== text ==============================
  /** 日志展示域 */
  @FXML private TextFlow logShowArea;

  /** 页码展示 */
  @FXML private TextField paginate;

  // endregion============================== text ==============================
  // region============================== button ============================

  /** 上一页 */
  @FXML private Button preLoadBtn;

  /** 下一页 */
  @FXML private Button nexLoadBtn;

  /** MP3转MIDI按钮 */
  @FXML private Button mp3ToMidiBtn;

  /** 生成按钮 */
  @FXML private Button generateBtn;

  // endregion============================== button ==============================
  // region============================== Pane =============================
  @FXML FlowPane soundDataPanel;

  /** 日志滚动条 */
  @FXML ScrollPane logShowScrollPane;

  // endregion============================== ScrollPane ==============================
  // region============================== container =============================
  // 文件选择label container
  @FXML HBox fileChooserContainer;
  @FXML Label fileChooserLabel;
  @FXML Pane background;

  // endregion============================== container ==============================

  /** 文字域日志 */
  private MainWindowTextAreaLogger textAreaLogger;

  /** 选择的音乐文件 */
  private File musicFileChoose;

  /** 文件解码之后的数据 */
  private MidiFileDecoder midiFileDecoder;

  /** 声音数据区渲染器 */
  private ScSoundDataPanelRender soundDataPanelRender;

  /** 当前是否处理完成mp3转midi的任务 */
  private final AtomicBoolean isHandleMp3ToMidiDone = new AtomicBoolean(true);

  public ScMusicGeneratorController() {}

  // region============================== 点击事件 =============================

  /** 描述点击 */
  @FXML
  protected void onDescribeClick() {
    WindowManager.getInstance().openWindowWithStage(stage, DescribeController.class);
  }

  @FXML
  protected void onFileSelectClick() {
    Window mainWindow = getStage().getOwner();
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("MIDI(.mid)文件或MP3(.mp3)文件选择");
    // 设置初始文件位置
    fileChooser.setInitialDirectory(
        new File(
            FileUtils.getRelatedPathOfRoot(DefaultEnvConfigConstant.DEFAULT_MUSIC_SELECT_PATH)));
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(".mid文件或.mp3文件", "*.mid", "*.mp3"));
    File selectedFile = fileChooser.showOpenDialog(mainWindow);
    if (selectedFile == null) {
      textAreaLogger.info("取消文件选择");
    } else {

      fileChooserLabel.setText(selectedFile.getName());
      Tooltip tooltip = new Tooltip();
      tooltip.setText(selectedFile.getName());
      fileChooserLabel.setTooltip(tooltip);

      String logStr = "已选择文件: " + selectedFile.getName();
      textAreaLogger.info(logStr);
      musicFileChoose = selectedFile;

      fileChooserContainer.setVisible(true);
      boolean isMp3 = selectedFile.getName().endsWith(".mp3");
      mp3ToMidiBtn.setVisible(isMp3);
      preLoadBtn.setDisable(true);
      nexLoadBtn.setDisable(true);
      generateBtn.setDisable(isMp3);
    }
  }

  @FXML
  private void onMp3ToMidiClick() {
    if (!isHandleMp3ToMidiDone.get()) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setContentText("当前Mp3转Midi任务未完成！");
      alert.show();
      return;
    }
    CompletableFuture.runAsync(
            () -> {
              try {
                Platform.runLater(
                    () -> {
                      fileChooserLabel.setText("正在转换中...");
                      textAreaLogger.info("开始转换(Start Converting)...");
                      fileChooserLabel.setStyle("-fx-text-fill: green");
                      mp3ToMidiBtn.setDisable(true);
                    });
                isHandleMp3ToMidiDone.set(false);
                musicFileChoose =
                    Mp3ToMidiDecoder.decodeMp3ToMidiFromApi(textAreaLogger, musicFileChoose);
                isHandleMp3ToMidiDone.set(true);
                Platform.runLater(() -> mp3ToMidiBtn.setDisable(false));
              } catch (Exception e) {
                LoggerUtils.showError("Mp3转Midi文件出错", e.getMessage(), e);
                throw new RuntimeException(e);
              }
            })
        .whenComplete(
            (res, throwable) -> {
              if (throwable == null) {
                Platform.runLater(
                    () -> {
                      mp3ToMidiBtn.setVisible(false);
                      generateBtn.setDisable(false);
                      fileChooserLabel.setText(musicFileChoose.getName());
                      Tooltip tooltip = new Tooltip();
                      tooltip.setText(musicFileChoose.getName());
                      fileChooserLabel.setTooltip(tooltip);
                    });
              } else {
                Platform.runLater(
                    () -> {
                      mp3ToMidiBtn.setDisable(false);
                      fileChooserLabel.setText(musicFileChoose.getName());
                    });
                textAreaLogger.error("转换出错：" + throwable.getMessage(), throwable);
              }
              isHandleMp3ToMidiDone.set(true);
            });
  }

  @FXML
  protected void onDonateClick() {
    WindowManager.getInstance().openWindowWithStage(stage, DonateController.class);
  }

  @FXML
  private void onAboutClick() {
    WindowManager.getInstance().openWindowWithStage(stage, AboutController.class);
  }

  @FXML
  private void onChangeBackground() throws IOException {
    Window mainWindow = getStage().getOwner();
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("选取背景图片");
    // 设置初始文件位置
    fileChooser.setInitialDirectory(new File(FileUtils.getRootDir()));
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(".png文件或.jpg文件", "*.jpg", "*.png"));
    File selectedFile = fileChooser.showOpenDialog(mainWindow);
    if (selectedFile == null) {
      textAreaLogger.info("取消文件选择");
    } else {
      background.setBackground(
          new Background(
              new BackgroundImage(
                  new Image(selectedFile.toURI().toURL().openStream()),
                  BackgroundRepeat.NO_REPEAT,
                  BackgroundRepeat.REPEAT,
                  BackgroundPosition.CENTER,
                  new BackgroundSize(
                      BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, true))));
      // 保存文件
      String backgroundImgPath =
          FileUtils.getRelatedPathOfRoot(DefaultEnvConfigConstant.BACKGROUND_IMG_CONFIG_PATH);
      File backgroundImgPathFile = new File(backgroundImgPath);
      File configDir =
          new File(FileUtils.getRelatedPathOfRoot(DefaultEnvConfigConstant.CONFIG_PATH));
      try {
        if (!configDir.exists()) {
          configDir.mkdirs();
        }
        BufferedWriter bufferedReader = new BufferedWriter(new FileWriter(backgroundImgPathFile));
        bufferedReader.write(selectedFile.getAbsolutePath());
        bufferedReader.flush();
        bufferedReader.close();
        textAreaLogger.info("背景修改成功！当前背景名: {}", selectedFile.getName());
      } catch (IOException ignored) {
      }
    }
  }

  @FXML
  protected void onPreBtnClick() {
    int oldPage = Integer.parseInt(paginate.getText());
    int currentPage = oldPage - 1;
    if (midiFileDecoder != null && oldPage > 1) {
      paginate.setText(currentPage + "");
      if (currentPage <= 1) {
        preLoadBtn.setDisable(true);
      }
      nexLoadBtn.setDisable(false);
      soundDataPanelRender.renderDataPanel(soundDataPanel, currentPage);
    }
  }

  @FXML
  protected void onNextBtnClick() {
    int oldPage = Integer.parseInt(paginate.getText());
    int currentPage = oldPage + 1;
    if (midiFileDecoder != null && oldPage < midiFileDecoder.getShowPanelMaxPage()) {
      paginate.setText((currentPage) + "");
      if (currentPage >= midiFileDecoder.getShowPanelMaxPage()) {
        nexLoadBtn.setDisable(true);
      }
      preLoadBtn.setDisable(false);
      soundDataPanelRender.renderDataPanel(soundDataPanel, currentPage);
    }
  }

  @FXML
  private void onMusicDataGenBtnClick() {
    double explosionX = stage.getX() + stage.getWidth() / 2;
    double explosionY = stage.getY() + stage.getHeight() / 2;
    EffectUtils.shakeWindow(
        stage, new EffectUtils.ShakeMeta(explosionX, explosionY, 25, 30, 50000));
    MediaUtils.musicPlay("sound/Explosion.wav");
    try {
      midiFileDecoder = new MidiFileDecoder();
      // 解析
      midiFileDecoder.decode(textAreaLogger, musicFileChoose);
      // 渲染第一页的数据格子
      soundDataPanelRender = new ScSoundDataPanelRender();
      if (soundDataPanelRender.renderPanel(textAreaLogger, midiFileDecoder)) {
        nexLoadBtn.setDisable(midiFileDecoder.getShowPanelMaxPage() == 1);
        preLoadBtn.setDisable(true);
        paginate.setText("1");
        soundDataPanelRender.renderDataPanel(soundDataPanel, 1);
        textAreaLogger.info("生成结束，生效的乐器数量: {}", midiFileDecoder.getInstrumentNeededNum());
      } else {
        paginate.setText("0");
        preLoadBtn.setDisable(true);
        nexLoadBtn.setDisable(true);
        soundDataPanelRender.renderEmptyPanelWithWords(soundDataPanel, "点击生成音乐数据以展示数据面板");
        textAreaLogger.info("生成失败");
      }
    } catch (Exception exception) {
      textAreaLogger.error("解析MIDI错误", exception);
    }
  }

  // endregion============================== 点击事件 ==============================

  @Override
  public void onMounted(Object... args) {
    this.textAreaLogger = new MainWindowTextAreaLogger(logShowArea);
    this.logShowScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    this.logShowScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    // 从文件中读取背景图片路径
    boolean useHistoryPic = false;
    String backgroundImgPath =
        FileUtils.getRelatedPathOfRoot(DefaultEnvConfigConstant.BACKGROUND_IMG_CONFIG_PATH);
    File backgroundImgPathFile = new File(backgroundImgPath);
    if (backgroundImgPathFile.exists()) {
      try {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(backgroundImgPathFile));
        String imgPath = bufferedReader.readLine();
        File backgroundImgFile = new File(imgPath);
        boolean isPicture = imgPath.endsWith(".jpg") || imgPath.endsWith(".png");
        if (backgroundImgFile.exists() && isPicture) {
          background.setBackground(
              new Background(
                  new BackgroundImage(
                      new Image(backgroundImgFile.toURI().toURL().openStream()),
                      BackgroundRepeat.NO_REPEAT,
                      BackgroundRepeat.REPEAT,
                      BackgroundPosition.CENTER,
                      new BackgroundSize(
                          BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, true))));
          useHistoryPic = true;
        }
      } catch (IOException ignored) {
      }
    }
    if (!useHistoryPic) {
      UiUtils.updateComponentBackgroundImg(
          this.background,
          FileUtils.getFullResourceUrl(DefaultEnvConfigConstant.DEFAULT_BACKGROUND_IMG));
    }
    BoxBlur boxBlur = new BoxBlur(10, 10, 10);
    this.background.setEffect(boxBlur);
    super.onMounted(args);
  }

  public MainWindowTextAreaLogger getTextAreaLogger() {
    return textAreaLogger;
  }

  @Override
  public void onCreate(Stage stage) {
    super.onCreate(stage);
    stage.setResizable(true);
    stage.widthProperty().addListener((observable, oldValue, newValue) -> stage.setWidth(815));
    stage.heightProperty().addListener((observable, oldValue, newValue) -> stage.setHeight(630));
  }

  public ScrollPane getLogShowScrollPane() {
    return logShowScrollPane;
  }

  @Override
  public String getFxmlPath() {
    return "sc-music-generator";
  }

  @Override
  public String getStageIconPath() {
    return "icon/main.png";
  }

  @Override
  public Stage getStage() {
    return stage;
  }

  @Override
  public String getTitle() {
    return "SurvivalCraft Music Data Generator(SC音乐数据生成工具) " + FileUtils.getAppVersion();
  }
}
