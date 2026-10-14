package com.eouna.scmusicgenerator.generator.midi;

import com.eouna.scmusicgenerator.constant.DefaultEnvConfigConstant;
import com.eouna.scmusicgenerator.core.logger.TextAreaLogger;
import com.eouna.scmusicgenerator.generator.midi.ScMusicData.ScPage;
import com.eouna.scmusicgenerator.generator.midi.ScMusicData.ScPlate;
import com.eouna.scmusicgenerator.utils.FileUtils;
import com.eouna.scmusicgenerator.utils.ToolsLoggerUtils;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * 音乐数据展示区渲染器: 每个声部一个九宫格, 四边分别是 V端 / P端 / O端 / 乐器端 的存储板数据.
 *
 * @author CCL
 */
public class ScSoundDataPanelRender {

  /** 存储板游戏内编辑框每行的字符数 */
  private static final int CHARS_PER_ROW = 16;

  /** 当前展示的数据 */
  private ScMusicData musicData;

  /**
   * 设置待展示的音乐数据
   *
   * @param musicData 音乐数据
   * @return 是否有可展示的数据
   */
  public boolean setMusicData(ScMusicData musicData) {
    this.musicData = musicData;
    return musicData != null && !musicData.isEmpty();
  }

  public ScMusicData getMusicData() {
    return musicData;
  }

  /** 总页数 */
  public int getPageCount() {
    return musicData == null ? 0 : musicData.getPageCount();
  }

  /** 渲染数据面板，当数据为空时 */
  public void renderEmptyPanelWithWords(FlowPane soundDataPanel, String words) {
    soundDataPanel.getChildren().clear();
    HBox container = new HBox();
    container.setPrefHeight(290);
    container.setPrefWidth(795);
    container.setAlignment(Pos.CENTER);

    Label label = new Label(words);
    label.setDisable(true);
    label.setPrefHeight(23);
    label.setPrefWidth(200);
    label.setStyle("-fx-text-fill: rgba(0,0,0,0.91)");

    container.getChildren().add(label);
    soundDataPanel.getChildren().add(container);
  }

  /**
   * 渲染数据展示区
   *
   * @param soundDataPanel 数据展示区引用
   * @param pageIdx 页数, 从 1 开始
   */
  public void renderDataPanel(FlowPane soundDataPanel, int pageIdx) {
    soundDataPanel.getChildren().clear();
    if (musicData == null || pageIdx < 1 || pageIdx > musicData.getPageCount()) {
      return;
    }
    // 向展示面板添加数据块, 所有声部的页数相同, 同一页在时间上是对齐的
    for (ScPlate plate : musicData.getAllPlates()) {
      ScPage page = plate.pages.get(pageIdx - 1);
      soundDataPanel.getChildren().add(buildDataGrid(plate, page));
    }
  }

  /**
   * 构建音乐数据展示面板
   *
   * @return 展示面板
   */
  private GridPane buildDataGrid(ScPlate plate, ScPage page) {
    GridPane gridPane = new GridPane();
    gridPane.setPrefHeight(144);
    gridPane.setPrefWidth(260);
    gridPane.setStyle("-fx-border-color: #EEE;-fx-border-image-width: 1px;margin:2px 2px 0 2px");
    // ____数据_____
    // 数据 乐器 数据
    // ____数据_____
    gridPane.add(getDisablePanel(), 0, 0);
    gridPane.add(getTextDataBlock(page.volumeData, "V端"), 1, 0);
    gridPane.add(getDisablePanel(), 2, 0);
    gridPane.add(getTextDataBlock(page.pitchData, "P端"), 0, 1);

    VBox instrumentNameBox = new VBox();
    Label instrumentNameText = buildInstrumentLabel(plate.getDisplayName());
    instrumentNameBox.getChildren().add(instrumentNameText);
    Label instrumentEnNameText = buildInstrumentLabel(plate.getDisplayEnName());
    instrumentNameBox.getChildren().add(instrumentEnNameText);
    Tooltip tooltip =
        new Tooltip(
            plate.getDisplayName()
                + " 第 "
                + (page.pageIndex + 1)
                + "/"
                + plate.pages.size()
                + " 页, 本页音符 "
                + page.noteCount
                + " 个, 本声部共 "
                + plate.noteCount
                + " 个");
    Tooltip.install(instrumentNameBox, tooltip);
    gridPane.add(instrumentNameBox, 1, 1);

    gridPane.add(getTextDataBlock(page.octaveData, "O端"), 2, 1);
    gridPane.add(getDisablePanel(), 0, 2);
    gridPane.add(getTextDataBlock(page.toneData, "乐器端"), 1, 2);
    gridPane.add(getDisablePanel(), 2, 2);
    return gridPane;
  }

  /**
   * 创建乐器文字
   *
   * @param instrumentName 乐器名
   * @return label
   */
  private Label buildInstrumentLabel(String instrumentName) {
    Label instrumentNameText = new Label(instrumentName);
    instrumentNameText.setPrefHeight(48);
    instrumentNameText.setPrefWidth(86);
    instrumentNameText.setOpacity(DefaultEnvConfigConstant.DEFAULT_MUSIC_DATA_PANEL_OPACITY);
    instrumentNameText.setContentDisplay(ContentDisplay.CENTER);
    instrumentNameText.setAlignment(Pos.CENTER);
    instrumentNameText.setFont(new Font(16));
    instrumentNameText.setStyle(
        "-fx-text-fill: #e90780;-fx-background-color: rgb(10,7,7);-fx-background-radius: 5px;-fx-border-width: 1px");
    return instrumentNameText;
  }

  private Pane getDisablePanel() {
    Pane disablePanel = new Pane();
    disablePanel.setPrefHeight(48);
    disablePanel.setDisable(true);
    return disablePanel;
  }

  /**
   * 生成可复制的数据展示块
   *
   * @param dataStr 音乐数据
   * @return 展示块
   */
  private VBox getTextDataBlock(String dataStr, String direct) {
    VBox vBox = new VBox();
    vBox.setPrefHeight(48);

    TextField textField = new TextField(dataStr);
    textField.setPrefHeight(20);
    textField.setOpacity(DefaultEnvConfigConstant.DEFAULT_MUSIC_DATA_PANEL_OPACITY);
    textField.setStyle("-fx-text-fill: #000");
    textField.setEditable(false);
    textField.setOnMouseClicked(
        (e) -> {
          Alert alertDialog = new Alert(Alert.AlertType.INFORMATION);
          alertDialog.setHeaderText("(" + direct + ")数据");
          alertDialog.setGraphic(
              new ImageView(new Image(FileUtils.getFullResourceUrl("icon/look.png"))));
          alertDialog.setContentText(formatAsMemoryBankRows(dataStr));
          alertDialog.setTitle("数据查看(DataViewer)");
          Stage alertStage = (Stage) alertDialog.getDialogPane().getScene().getWindow();
          alertStage.setOpacity(0.7);
          alertDialog.showAndWait();
        });
    vBox.getChildren().add(textField);

    Button copyDataBtn = new Button("(" + direct + ")" + "复制");
    copyDataBtn.setPrefHeight(24);
    copyDataBtn.setPrefWidth(86);
    copyDataBtn.setOnMouseClicked((e) -> copyData(dataStr));
    copyDataBtn.setStyle(
        "-fx-text-fill: #000;-fx-opacity: "
            + DefaultEnvConfigConstant.DEFAULT_MUSIC_DATA_PANEL_OPACITY);
    vBox.getChildren().add(copyDataBtn);
    return vBox;
  }

  /** 按游戏内存储板编辑框的样子, 每 16 个字符一行 */
  static String formatAsMemoryBankRows(String dataStr) {
    int rows = (int) Math.ceil(dataStr.length() / (double) CHARS_PER_ROW);
    StringBuilder formatDataStr = new StringBuilder();
    for (int i = 0; i < rows; i++) {
      formatDataStr
          .append(dataStr, i * CHARS_PER_ROW, Math.min(dataStr.length(), CHARS_PER_ROW * (i + 1)))
          .append("\r\n");
    }
    return formatDataStr.toString();
  }

  private void copyData(String needCopiedText) {
    if (needCopiedText == null) {
      return;
    }
    ClipboardContent content = new ClipboardContent();
    content.putString(needCopiedText);
    if (Clipboard.getSystemClipboard().setContent(content)) {
      TextAreaLogger logger = ToolsLoggerUtils.getMainTextAreaLog();
      if (logger != null) {
        logger.success("复制成功(Copied Success)");
      }
    }
  }
}
