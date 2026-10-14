package com.eouna.scmusicgenerator.ui.controllers;

import com.eouna.scmusicgenerator.constant.DefaultEnvConfigConstant;
import com.eouna.scmusicgenerator.core.logger.LoggerUtils;
import com.eouna.scmusicgenerator.core.window.BaseWindowController;
import com.eouna.scmusicgenerator.core.window.MainWindowIdentifier;
import com.eouna.scmusicgenerator.core.window.WindowManager;
import com.eouna.scmusicgenerator.generator.midi.MidiFileDecoder;
import com.eouna.scmusicgenerator.generator.midi.QuantizeStep;
import com.eouna.scmusicgenerator.generator.midi.ScMusicData;
import com.eouna.scmusicgenerator.generator.midi.ScMusicDataBuilder;
import com.eouna.scmusicgenerator.generator.midi.ScMusicPreviewPlayer;
import com.eouna.scmusicgenerator.generator.midi.ScSoundDataPanelRender;
import com.eouna.scmusicgenerator.generator.midi.ToneMode;
import com.eouna.scmusicgenerator.generator.mp3.Mp3ToMidiDecoder;
import com.eouna.scmusicgenerator.utils.*;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * 音乐数据生成展示窗口
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

  /** 预播放按钮 */
  @FXML private Button previewBtn;

  /** 预播放按钮图标 */
  @FXML private FontIcon previewIcon;

  /** 预播放进度 */
  @FXML private Label previewProgressLabel;

  // endregion============================== button ==============================
  // region============================== 生成选项 =============================

  /** 量化步长 */
  @FXML private ComboBox<QuantizeStep> quantizeStepBox;

  /** 乐器端模式 */
  @FXML private ComboBox<ToneMode> toneModeBox;

  /** 响度归一化 */
  @FXML private CheckBox normalizeVelocityBox;

  // endregion============================== 生成选项 ==============================
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

  /** 生成好的音乐数据 */
  private ScMusicData musicData;

  /** 声音数据区渲染器 */
  private final ScSoundDataPanelRender soundDataPanelRender = new ScSoundDataPanelRender();

  /** 预播放器: 模拟发音器电路发声 */
  private final ScMusicPreviewPlayer previewPlayer = new ScMusicPreviewPlayer();

  /** 预播放进度刷新定时器 */
  private Timeline previewProgressTimeline;

  /** 预播放是否正在初始化 */
  private final AtomicBoolean previewStarting = new AtomicBoolean(false);

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
    fileChooser.setTitle("MIDI(.mid/.midi)文件或MP3(.mp3)文件选择");
    // 设置初始文件位置
    fileChooser.setInitialDirectory(
        new File(
            FileUtils.getRelatedPathOfRoot(DefaultEnvConfigConstant.DEFAULT_MUSIC_SELECT_PATH)));
    fileChooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                ".mid文件或.mp3文件", "*.mid", "*.midi", "*.MID", "*.MIDI", "*.mp3", "*.MP3"));
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
      boolean isMp3 = selectedFile.getName().toLowerCase().endsWith(".mp3");
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
    int oldPage = getShownPage();
    if (musicData != null && oldPage > 1) {
      showPage(oldPage - 1);
    }
  }

  @FXML
  protected void onNextBtnClick() {
    int oldPage = getShownPage();
    if (musicData != null && oldPage < musicData.getPageCount()) {
      showPage(oldPage + 1);
    }
  }

  /** 当前展示的页码(从 1 开始), 没有数据时为 0 */
  private int getShownPage() {
    try {
      return Integer.parseInt(paginate.getText().trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * 展示指定页并刷新翻页按钮状态
   *
   * @param page 页码, 从 1 开始
   */
  private void showPage(int page) {
    if (musicData == null || musicData.isEmpty()) {
      return;
    }
    int target = Math.max(1, Math.min(musicData.getPageCount(), page));
    paginate.setText(String.valueOf(target));
    preLoadBtn.setDisable(target <= 1);
    nexLoadBtn.setDisable(target >= musicData.getPageCount());
    soundDataPanelRender.renderDataPanel(soundDataPanel, target);
  }

  /** 读取界面上的生成选项 */
  private ScMusicDataBuilder.Options collectBuildOptions() {
    ScMusicDataBuilder.Options options = new ScMusicDataBuilder.Options();
    QuantizeStep step = quantizeStepBox.getValue();
    if (step != null) {
      options.setStep(step);
    }
    ToneMode toneMode = toneModeBox.getValue();
    if (toneMode != null) {
      options.setToneMode(toneMode);
    }
    options.setNormalizeVelocity(normalizeVelocityBox.isSelected());
    return options;
  }

  @FXML
  private void onMusicDataGenBtnClick() {
    if (musicFileChoose == null) {
      textAreaLogger.warn("请先选择MIDI文件");
      return;
    }
    if (previewStarting.get()) {
      textAreaLogger.warn("预播放正在初始化, 请稍候");
      return;
    }
    // 重新生成前停止预播放
    if (previewPlayer.isPlaying()) {
      stopPreview(true);
    }
    double explosionX = stage.getX() + stage.getWidth() / 2;
    double explosionY = stage.getY() + stage.getHeight() / 2;
    EffectUtils.shakeWindow(
        stage, new EffectUtils.ShakeMeta(explosionX, explosionY, 25, 30, 50000));
    MediaUtils.musicPlay("sound/Explosion.wav");
    try {
      midiFileDecoder = new MidiFileDecoder();
      // 解析
      midiFileDecoder.decode(textAreaLogger, musicFileChoose);
      // 按统一时间网格生成存储板数据
      musicData =
          new ScMusicDataBuilder().build(textAreaLogger, midiFileDecoder, collectBuildOptions());
      // 渲染第一页的数据格子
      if (soundDataPanelRender.setMusicData(musicData)) {
        showPage(1);
        previewBtn.setDisable(false);
        textAreaLogger.success(
            "生成结束, 乐器 {} 种, 发音器 {} 块, 每块 {} 页; 时钟周期 {} 秒",
            musicData.getPlates().size(),
            musicData.getPlateCount(),
            musicData.getPageCount(),
            String.format("%.4f", musicData.getCellPeriodSeconds()));
        textAreaLogger.info("点击 预播放(Preview) 可以模拟发音器电路试听");
      } else {
        paginate.setText("0");
        preLoadBtn.setDisable(true);
        nexLoadBtn.setDisable(true);
        previewBtn.setDisable(true);
        soundDataPanelRender.renderEmptyPanelWithWords(soundDataPanel, "点击生成音乐数据以展示数据面板");
        textAreaLogger.info("生成失败");
      }
    } catch (Exception exception) {
      musicData = null;
      previewBtn.setDisable(true);
      textAreaLogger.error("解析MIDI错误", exception);
    }
  }

  // endregion============================== 点击事件 ==============================
  // region============================== 预播放 =============================

  /** 预播放 / 停止 */
  @FXML
  private void onPreviewBtnClick() {
    if (previewStarting.get()) {
      return;
    }
    if (previewPlayer.isPlaying()) {
      stopPreview(true);
      return;
    }
    if (musicData == null || musicData.isEmpty()) {
      textAreaLogger.warn("请先生成音乐数据");
      return;
    }
    ScMusicData dataToPlay = musicData;
    previewStarting.set(true);
    setPreviewButtonPlaying(true);
    previewBtn.setDisable(true);
    previewProgressLabel.setText("正在初始化合成器...");
    previewProgressLabel.setVisible(true);
    // 第一次需要初始化软件合成器, 放到后台线程避免界面卡顿
    CompletableFuture.runAsync(
        () -> {
          try {
            previewPlayer.play(
                textAreaLogger,
                dataToPlay,
                () ->
                    Platform.runLater(
                        () -> {
                          textAreaLogger.info("预播放结束");
                          stopPreview(false);
                        }));
            Platform.runLater(
                () -> {
                  previewBtn.setDisable(false);
                  startPreviewProgressTimer();
                });
          } catch (Exception e) {
            Platform.runLater(
                () -> {
                  textAreaLogger.error("预播放失败, 没有可用的 MIDI 合成器或音频设备: " + e.getMessage(), e);
                  stopPreview(false);
                });
          } finally {
            previewStarting.set(false);
          }
        });
  }

  /**
   * 停止预播放并恢复界面
   *
   * @param stopPlayer 是否需要停止播放器(自然结束时不需要)
   */
  private void stopPreview(boolean stopPlayer) {
    stopPreviewProgressTimer();
    if (stopPlayer) {
      previewPlayer.stop();
      textAreaLogger.info("预播放已停止");
    }
    setPreviewButtonPlaying(false);
    previewProgressLabel.setVisible(false);
    previewBtn.setDisable(musicData == null || musicData.isEmpty());
  }

  private void setPreviewButtonPlaying(boolean playing) {
    previewBtn.setText(playing ? "停止(Stop)" : "预播放(Preview)");
    if (previewIcon != null) {
      previewIcon.setIconLiteral(playing ? "fa-stop" : "fa-play");
    }
    previewBtn.getStyleClass().remove("playing");
    if (playing) {
      previewBtn.getStyleClass().add("playing");
    }
  }

  private void startPreviewProgressTimer() {
    stopPreviewProgressTimer();
    previewProgressTimeline =
        new Timeline(new KeyFrame(Duration.millis(100), e -> updatePreviewProgress()));
    previewProgressTimeline.setCycleCount(Animation.INDEFINITE);
    previewProgressTimeline.play();
  }

  private void stopPreviewProgressTimer() {
    if (previewProgressTimeline != null) {
      previewProgressTimeline.stop();
      previewProgressTimeline = null;
    }
  }

  /** 刷新进度文字, 并把数据面板翻到正在播放的页 */
  private void updatePreviewProgress() {
    if (musicData == null || musicData.isEmpty() || !previewPlayer.isPlaying()) {
      // 播放已经结束(很短的乐曲可能在定时器启动前就播完了)
      stopPreview(false);
      return;
    }
    int cellsPerStep = musicData.getToneMode().getCellsPerStep();
    int cellsPerPage = musicData.getStepsPerPage() * cellsPerStep;
    long cell = previewPlayer.getCurrentCell();
    int step = (int) Math.min(musicData.getTotalSteps(), cell / cellsPerStep + 1);
    double totalSeconds = previewPlayer.getTotalCells() * previewPlayer.getCellPeriodSeconds();
    previewProgressLabel.setText(
        String.format(
            "第 %d/%d 步  %s / %s",
            step,
            musicData.getTotalSteps(),
            formatSeconds(Math.min(previewPlayer.getCurrentSeconds(), totalSeconds)),
            formatSeconds(totalSeconds)));
    int playingPage = (int) Math.min(musicData.getPageCount(), cell / cellsPerPage + 1);
    if (playingPage != getShownPage()) {
      showPage(playingPage);
    }
  }

  private static String formatSeconds(double seconds) {
    int total = (int) Math.round(seconds);
    return String.format("%d:%02d", total / 60, total % 60);
  }

  // endregion============================== 预播放 ==============================

  @Override
  public void onMounted(Object... args) {
    this.textAreaLogger = new MainWindowTextAreaLogger(logShowArea);
    this.logShowScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    this.logShowScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    // 生成选项
    this.quantizeStepBox.getItems().setAll(QuantizeStep.presets());
    this.quantizeStepBox.setValue(QuantizeStep.defaultStep());
    this.quantizeStepBox.setTooltip(
        new Tooltip("存储板一个格子代表的时长. 按拍: 随 MIDI 速度换算; 固定毫秒: 与速度无关.\n步长越小节奏越精确, 但占用的存储板越多"));
    this.toneModeBox.getItems().setAll(ToneMode.values());
    this.toneModeBox.setValue(ToneMode.CONSTANT);
    this.toneModeBox.setTooltip(
        new Tooltip(
            "发音器只在乐器端 0->非0 时发声.\n恒定: 乐器端数据需与时钟脉冲(F)过 AND 门;\n脉冲: 每个音后补一格 0, 不需要 AND 门, 但每块板只能存 128 步"));
    this.normalizeVelocityBox.setTooltip(new Tooltip("把乐曲中最响的音放大到 F, 其余按比例缩放"));
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
  protected void onClose() {
    super.onClose();
    stopPreviewProgressTimer();
    previewPlayer.close();
  }

  @Override
  public void beforeDestroy() {
    super.beforeDestroy();
    stopPreviewProgressTimer();
    previewPlayer.close();
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
