package com.eouna.scmusicgenerator.generator.midi;

import com.eouna.scmusicgenerator.constant.DefaultEnvConfigConstant;
import com.eouna.scmusicgenerator.core.logger.TextAreaLogger;
import com.eouna.scmusicgenerator.generator.midi.NoteDataCollection.ScNoteData;
import com.eouna.scmusicgenerator.utils.FileUtils;
import com.eouna.scmusicgenerator.utils.MainWindowTextAreaLogger;
import com.eouna.scmusicgenerator.utils.ToolsLoggerUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
 * @author CCL
 */
public class ScSoundDataPanelRender {

  /** SC乐器别名映射 */
  private static final Map<Integer, String> INSTRUMENT_ALIAS_NAME = new HashMap<>();

  private static final Map<Integer, String> INSTRUMENT_EN_ALIAS_NAME = new HashMap<>();

  private static final char[] SC_NOTES =
      new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  private static final char[] SC_VELOCITY =
      new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  private static final char PAUSE = 'F';

  /** 展示区域的音乐数据 乐器名 <=> sc音乐数据列表 每个GridNoteData保存着一个音乐板的数据 即256个字符 */
  private final Map<NoteNameTuple, List<GridNoteData>> noteDataMap = new LinkedHashMap<>();

  static {
    INSTRUMENT_ALIAS_NAME.put(1, "铃铛");
    INSTRUMENT_ALIAS_NAME.put(2, "风琴");
    INSTRUMENT_ALIAS_NAME.put(3, "砰");
    INSTRUMENT_ALIAS_NAME.put(4, "弦乐器");
    INSTRUMENT_ALIAS_NAME.put(5, "小号");
    INSTRUMENT_ALIAS_NAME.put(6, "语音");
    INSTRUMENT_ALIAS_NAME.put(7, "钢琴");
    INSTRUMENT_ALIAS_NAME.put(8, "钢琴延音");
    INSTRUMENT_ALIAS_NAME.put(9, "鼓");
    INSTRUMENT_ALIAS_NAME.put(10, "贝斯");

    INSTRUMENT_EN_ALIAS_NAME.put(1, "Bell");
    INSTRUMENT_EN_ALIAS_NAME.put(2, "Organ");
    INSTRUMENT_EN_ALIAS_NAME.put(3, "Ping");
    INSTRUMENT_EN_ALIAS_NAME.put(4, "String");
    INSTRUMENT_EN_ALIAS_NAME.put(5, "Trumpet");
    INSTRUMENT_EN_ALIAS_NAME.put(6, "Voice");
    INSTRUMENT_EN_ALIAS_NAME.put(7, "Piano");
    INSTRUMENT_EN_ALIAS_NAME.put(8, "PianoSustain");
    INSTRUMENT_EN_ALIAS_NAME.put(9, "Trumps");
    INSTRUMENT_EN_ALIAS_NAME.put(10, "Bass");
  }

  /**
   * 渲染音乐数据面板
   *
   * @param fileDecoder 文件解码器
   * @return 是否成功
   */
  public boolean renderPanel(MainWindowTextAreaLogger logger, MidiFileDecoder fileDecoder) {
    Map<Integer, NoteDataCollection> scNoteData = fileDecoder.getScNoteData();
    if (scNoteData.isEmpty()) {
      return false;
    }
    Map<Integer, Integer> gmToCustom = MidiFileDecoder.getGmToCustom();
    int maxPage = 0;
    // 一次性生成所有数据
    for (Map.Entry<Integer, NoteDataCollection> entry : scNoteData.entrySet()) {
      int originInstrumentCode = entry.getKey();
      int scInstrumentCode = gmToCustom.get(originInstrumentCode);
      // 构造SC音乐板所需的数据结构，并使用列表分页
      List<List<GridNoteData>> gridNoteDataList =
          buildGridNoteDateByTick(
              logger, scInstrumentCode, fileDecoder.getResolution(), entry.getValue());
      Optional<Integer> gridNotePageSizeOp =
          gridNoteDataList.stream()
              .map(List::size)
              .mapToInt(o -> o)
              .boxed()
              .max(Integer::compareTo);
      int gridNotePageSize = gridNotePageSizeOp.orElse(1);
      if (gridNotePageSize > maxPage) {
        maxPage = gridNotePageSize;
      }
      for (int num = 0; num < gridNoteDataList.size(); num++) {
        List<GridNoteData> gridNoteDatas = gridNoteDataList.get(num);
        noteDataMap
            .computeIfAbsent(new NoteNameTuple(scInstrumentCode, num), k -> new ArrayList<>())
            .addAll(gridNoteDatas);
      }
    }
    fileDecoder.setShowPanelMaxPage(maxPage);
    return true;
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
   * @param pageIdx 页数
   */
  public void renderDataPanel(FlowPane soundDataPanel, int pageIdx) {
    soundDataPanel.getChildren().clear();
    // 向展示面板添加数据块
    for (Map.Entry<NoteNameTuple, List<GridNoteData>> entry : noteDataMap.entrySet()) {
      if (!entry.getValue().isEmpty() && pageIdx <= entry.getValue().size()) {
        GridPane gridPane = buildDataGrid(entry.getKey(), entry.getValue().get(pageIdx - 1));
        soundDataPanel.getChildren().add(gridPane);
      }
    }
  }

  /**
   * 构建音乐数据展示面板
   *
   * @return 展示面板
   */
  private GridPane buildDataGrid(NoteNameTuple noteNameTuple, GridNoteData gridNoteData) {
    GridPane gridPane = new GridPane();
    gridPane.setPrefHeight(144);
    gridPane.setPrefWidth(260);
    gridPane.setStyle("-fx-border-color: #EEE;-fx-border-image-width: 1px;margin:2px 2px 0 2px");
    // ____数据_____
    // 数据 乐器 数据
    // ____数据_____
    gridPane.add(getDisablePanel(), 0, 0);
    gridPane.add(getTextDataBlock(gridNoteData.velocityStrData.toString(), "V端"), 1, 0);
    gridPane.add(getDisablePanel(), 2, 0);
    gridPane.add(getTextDataBlock(gridNoteData.pitchStrData.toString(), "P端"), 0, 1);

    String instrumentName =
        INSTRUMENT_ALIAS_NAME.getOrDefault(noteNameTuple.instrumentCode, "未知乐器：")
            + (noteNameTuple.num + 1);
    VBox instrumentNameBox = new VBox();
    Label instrumentNameText = buildInstrumentLabel(instrumentName);
    instrumentNameBox.getChildren().add(instrumentNameText);
    String instrumentEnName =
        INSTRUMENT_EN_ALIAS_NAME.getOrDefault(
            noteNameTuple.instrumentCode, "Unknown：" + noteNameTuple.num);
    Label instrumentEnNameText = buildInstrumentLabel(instrumentEnName);
    instrumentNameBox.getChildren().add(instrumentEnNameText);
    gridPane.add(instrumentNameBox, 1, 1);

    gridPane.add(getTextDataBlock(gridNoteData.octavesStrData.toString(), "O端"), 2, 1);
    gridPane.add(getDisablePanel(), 0, 2);
    gridPane.add(getTextDataBlock(gridNoteData.instrumentStrData.toString(), "乐器端"), 1, 2);
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
          int multi = (int) Math.ceil(dataStr.length() / 16.0);
          StringBuilder formatDataStr = new StringBuilder();
          for (int i = 0; i < multi; i++) {
            formatDataStr
                .append(dataStr, i * 16, Math.min(dataStr.length(), 16 * (i + 1)))
                .append("\r\n");
          }
          alertDialog.setGraphic(
              new ImageView(new Image(FileUtils.getFullResourceUrl("icon/look.png"))));
          alertDialog.setContentText(formatDataStr.toString());
          alertDialog.setTitle("数据查看(DateViewer)");
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

  /**
   * 构造sc音乐板需要的数据格式
   *
   * @param baseResolution 基础频率
   * @param dataCollection 原始tick数据
   */
  private List<List<GridNoteData>> buildGridNoteDateByTick(
      MainWindowTextAreaLogger logger,
      int instrumentCode,
      int baseResolution,
      NoteDataCollection dataCollection) {
    TreeMap<Long, List<ScNoteData>> tickData = dataCollection.getNoteData();
    Optional<Integer> instrumentNeedOp =
        tickData.values().stream().map(List::size).mapToInt(o -> o).boxed().max(Integer::compareTo);
    // 需要的乐器数量
    int instrumentNeed = instrumentNeedOp.orElse(1);
    Map<Integer, List<GridNoteData>> gridNoteDataPaginate = new HashMap<>(instrumentNeed);
    int octaveDiff = dataCollection.getMaxOctave() - dataCollection.getMinOctave();
    AtomicInteger writeRec = new AtomicInteger(0);
    List<Long> scNoteTickList = new ArrayList<>(tickData.keySet());
    int baseTick = getBaseTick(120, baseResolution, 30);
    // 记录每组乐器note的数量，后续按照note数量进行排序
    Map<Integer, Integer> noteNumRec = new HashMap<>(instrumentNeed);
    for (int num = 0; num < instrumentNeed; num++) {
      List<GridNoteData> gridNoteDataList = new ArrayList<>();
      gridNoteDataPaginate.put(num, gridNoteDataList);
      int writeCounter = 0;
      noteNumRec.put(num, 0);
      // 按照基础tick进行保存数据
      GridNoteData gridNoteData = new GridNoteData();
      for (Long tick : scNoteTickList) {
        List<ScNoteData> scNoteDatas = tickData.get(tick);
        ScNoteData scNoteData;
        if (scNoteDatas.size() > num) {
          scNoteData = scNoteDatas.get(num);
          noteNumRec.put(num, noteNumRec.getOrDefault(num, 0) + 1);
        } else {
          // 空白填充
          scNoteData = new ScNoteData(15, 0, 0);
        }
        saveNoteData(
            scNoteData, gridNoteData, instrumentCode, writeRec, octaveDiff, dataCollection);
        // 每256字符后重新写入数据
        if (++writeCounter >= DefaultEnvConfigConstant.SC_MUSIC_BLOCK_MAX_STORAGE_CHAR) {
          gridNoteDataList.add(gridNoteData);
          gridNoteData = new GridNoteData();
          writeCounter = 0;
        }
      }
      if (writeCounter != 0) {
        gridNoteDataList.add(gridNoteData);
      }
    }
    logger.info(
        "乐器：{} 演奏数量: {} 最高八度：{} 最低八度：{}, 最高响度：{} 开始帧：{} 结束帧：{} 有效tick：{}",
        INSTRUMENT_ALIAS_NAME.get(instrumentCode),
        instrumentNeed,
        dataCollection.getMaxOctave(),
        dataCollection.getMinOctave(),
        dataCollection.getMaxVelocity(),
        tickData.firstKey(),
        tickData.lastKey(),
        writeRec.get());
    List<Map.Entry<Integer, Integer>> sortedMapEntry = new ArrayList<>(noteNumRec.entrySet());
    sortedMapEntry.sort(
        Comparator.comparingInt((Map.Entry<Integer, Integer> o) -> o.getValue()).reversed());
    LinkedHashMap<Integer, List<GridNoteData>> sortedData =
        sortedMapEntry.stream()
            .map(Map.Entry::getKey)
            .collect(
                LinkedHashMap::new,
                (linkedHashMap, integer) ->
                    linkedHashMap.put(integer, gridNoteDataPaginate.get(integer)),
                LinkedHashMap::putAll);
    return new ArrayList<>(sortedData.values());
  }

  /**
   * 保存note数据
   *
   * @param scNoteData 数据
   * @param gridNoteData 格子数据
   * @param instrumentCode 乐器id
   * @param writeRec 写入记录次数
   * @param diffOctave 当前乐器最高最低的八度差
   */
  private void saveNoteData(
      ScNoteData scNoteData,
      GridNoteData gridNoteData,
      int instrumentCode,
      AtomicInteger writeRec,
      int diffOctave,
      NoteDataCollection noteDataCollection) {
    // 当前乐器中最小的八度
    int minOctave = noteDataCollection.getMinOctave();
    gridNoteData.pitchStrData.append(SC_NOTES[scNoteData.pitch]);
    int velocity = scNoteData.velocity;
    if (noteDataCollection.getMaxVelocity() != SC_VELOCITY.length && velocity != 0) {
      velocity += SC_VELOCITY.length - noteDataCollection.getMaxVelocity();
    }
    gridNoteData.velocityStrData.append(SC_VELOCITY[Math.max(0, velocity - 1)]);
    gridNoteData.instrumentStrData.append(instrumentCode);
    int octave = scNoteData.octaves;
    // 钢琴 0-3
    if (instrumentCode == 7 || instrumentCode == 8) {
      double perOctaveTake = 4.0 / (diffOctave + 1);
      octave = (int) Math.floor((octave - minOctave) * perOctaveTake);
      // 下一个八度需要额外减一
      octave = scNoteData.pitch >= 12 ? Math.max(0, octave - 1) : octave;
    } else {
      // 其他乐器 八度范围0-2
      double perOctaveTake = 3.0 / (diffOctave + 1);
      octave = Math.max(0, Math.min(2, (int) Math.floor((octave - minOctave) * perOctaveTake)));
    }
    gridNoteData.octavesStrData.append(SC_NOTES[octave]);
    writeRec.incrementAndGet();
  }

  /**
   * 获取基础tick
   *
   * @param bpm 每分钟演奏
   * @param resolution 音频分辨率
   * @param scTickSecTimeGap 游戏脉冲电路的时间间隔(微秒)
   * @return 基础tick
   */
  private int getBaseTick(int bpm, int resolution, int scTickSecTimeGap) {
    int secOfQuarter = bpm / 60;
    int secOfQuarterTick = secOfQuarter * resolution;
    return (int) Math.floor((secOfQuarterTick) / (1000 / (scTickSecTimeGap * 1.0)));
  }

  static class GridNoteData {
    public StringBuilder pitchStrData = new StringBuilder();
    public StringBuilder velocityStrData = new StringBuilder();
    public StringBuilder octavesStrData = new StringBuilder();
    public StringBuilder instrumentStrData = new StringBuilder();
  }

  static class NoteNameTuple {
    int num;
    int instrumentCode;

    public NoteNameTuple(int instrumentCode, int num) {
      this.num = num;
      this.instrumentCode = instrumentCode;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      NoteNameTuple that = (NoteNameTuple) o;
      return num == that.num && instrumentCode == that.instrumentCode;
    }

    @Override
    public int hashCode() {
      return Objects.hash(num, instrumentCode);
    }
  }
}
