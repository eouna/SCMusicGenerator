package com.eouna.scmusicgenerator.ui.controllers;

import com.eouna.scmusicgenerator.core.window.BaseWindowController;
import com.eouna.scmusicgenerator.utils.FileUtils;
import com.eouna.scmusicgenerator.utils.NodeUtils;
import com.eouna.scmusicgenerator.utils.UiUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

/**
 * @author CCL
 */
public class DescribeController extends BaseWindowController {

  @FXML TextFlow main;

  @FXML AnchorPane blurBackground;

  /** 赋值标志位表达式 */
  private final Pattern paramFlagPattern = Pattern.compile("\\$\\{(.*?)\\}");

  @Override
  public void onMounted(Object... args) {
    super.onMounted(args);
    UiUtils.updateComponentBackgroundImg(
        blurBackground, FileUtils.getFullResourceUrl("img/describe_img.png"));
    BoxBlur boxBlur = new BoxBlur(5, 5, 15);
    this.blurBackground.setEffect(boxBlur);
    String text =
        "【功能说明】: 将.mid文件和.mp3文件解析成生存战争中可用的音乐板(发音器)数据\n\n"
            + "【使用方法】: 选择文件 -> 选好步长/乐器端模式 -> 点击生成, 然后把每个声部的 4 组数据分别复制到接在发音器"
            + " P端 / O端 / V端 / 乐器端 上的存储板(M板)里; 同一声部的多页按顺序接在后面, 所有声部的第 N 页在时间上是对齐的\n\n"
            + "【时间网格】: 存储板的 1 个格子 = 1 个量化步长(默认 1/4 拍), 所有乐器共用同一条时间轴, 格子序号就是时刻;"
            + " 长音 = 1 个发音格 + 若干休止格(P=F, V=0). 日志中会给出电路时钟的周期(秒), 请把计数器的时钟调成这个周期,"
            + " 不要用脉冲电路的时间去\"猜\"节奏\n\n"
            + "【数据说明】: \n"
            + "  P端(左) 音高 0~E, F = 休止; O端(右) 八度; V端(上) 响度 0~F; 乐器端(下) 乐器码\n"
            + "  游戏公式: 频率 = 130.8125Hz × 2^((P + 12×O)/12), 即 MIDI音符 = 48 + 12×O + P, 八度 0 = C3, 八度 1 = 中央C(C4) 所在八度\n"
            + "  O端范围: 风琴(2) 0~2, 其余乐器 0~3; 最高八度配合 P=C(12) 可多发一个 C(钢琴 = C7); 砰(3)/语音(6) 的采样实际比名义高一个八度, 本工具已按此修正\n"
            + "  乐器码: 1 铃铛 2 风琴 3 砰 4 弦乐 5 小号 6 语音 7 钢琴 8 钢琴延音 9 鼓 A 贝斯 (F 也是钢琴)\n"
            + "  鼓(9) 忽略 O端, P端选鼓声: 0 军鼓 1 底鼓 2 闭镲 3 踩镲 4 开镲 5 低音桶鼓 6 高音桶鼓 7 吊镲 8 叮叮镲 9 拍手\n\n"
            + "【乐器端是触发端】: 发音器只在乐器端输入从 0 变为非 0 (上升沿) 的瞬间发声, 两次触发间隔需 ≥ 0.08 秒\n"
            + "  恒定模式(默认): 乐器端数据全是乐器码, 接线时必须把乐器端存储板的输出与时钟脉冲过一个 AND 门再接发音器;"
            + " AND 门是 4bit 按位与且只能两个输入, 所以时钟电平必须是 F(1.5V). 钢琴可以不用乐器端存储板, 直接把时钟脉冲(F=钢琴)接到乐器端\n"
            + "  脉冲模式: 每个步长占两格, 音符后面自动补一格 0, 不需要 AND 门; 代价是每块板只能存 128 步, 时钟周期减半\n\n"
            + "【音域处理】: 乐器音域(C3~C7, 风琴到 C6)之外的音会整体按八度移调, 仍超出的音折叠回音域内, 日志里会说明; 同一格内同一乐器的多个音会分配到多个发音器(声部)\n\n"
            + "【参考资料】: \n"
            + "  发音器 Wiki[${SoundGeneratorWiki}]\n"
            + "  生存战争社区[${SurvivalCraftCommunity}]\n"
            + "  MIDI视频介绍[${MIDI_Introduce}]\n"
            + "  理解 MIDI 协议与标准 MIDI 文件格式[${UnderstandMidi}]\n"
            + "  MIDI格式解析[${MIDIProtocolDecode}]\n"
            + "  MIDI乐器编号[${MIDIInstrumentCodeNum}]\n"
            + "【MIDI补充】：\n"
            + "  Sequence.getResolution-即每个四分音符对应的 tick 数\n"
            + "resolution = 480：说明 1 个四分音符是 480 tick。\n"
            + "  BPM-每分钟多少个“拍子(一个4分音符)”\n"
            + "【免费MIDI资源网站】：\n"
            + "  ${MIDIResource1}\n"
            + "  ${MIDIResource2}\n"
            + "【音高图】：\n";
    Map<String, Node> jumpLinkMap = new HashMap<>(8);
    jumpLinkMap.put(
        "SoundGeneratorWiki",
        NodeUtils.createJumpLink("https://survivalcraft.miraheze.org/wiki/Sound_Generator"));
    jumpLinkMap.put(
        "SurvivalCraftCommunity",
        NodeUtils.createJumpLink("https://www.tapatalk.com/groups/survivalcraft/"));
    jumpLinkMap.put(
        "MIDI_Introduce", NodeUtils.createJumpLink("https://www.bilibili.com/video/BV1tu4y1N7gq"));
    jumpLinkMap.put(
        "UnderstandMidi",
        NodeUtils.createJumpLink("https://blog.csdn.net/ByteDanceTech/article/details/124358016"));
    jumpLinkMap.put(
        "MIDIProtocolDecode",
        NodeUtils.createJumpLink("https://blog.csdn.net/u010180372/article/details/118697564"));
    jumpLinkMap.put(
        "MIDIInstrumentCodeNum",
        NodeUtils.createJumpLink("https://zh.wikipedia.org/wiki/General_MIDI"));
    jumpLinkMap.put("MIDIResource1", NodeUtils.createJumpLink("https://www.kunstderfuge.com/"));
    jumpLinkMap.put(
        "MIDIResource2", NodeUtils.createJumpLink("https://onlinesequencer.net/sequences"));

    parseNode(text.split("\n"), jumpLinkMap, main);
    main.getChildren()
        .add(
            new ImageView(
                new Image(
                    FileUtils.getFullResourceUrl("img/pitch_number.png"),
                    512,
                    390,
                    true,
                    true,
                    true)));
  }

  private void parseNode(
      String[] configExplainStrArr, Map<String, Node> configExplainReplaceMap, TextFlow textFlow) {
    for (String line : configExplainStrArr) {
      Matcher matcher = paramFlagPattern.matcher(line);
      if (matcher.find()) {
        List<Node> lineNode = new ArrayList<>();
        matcher.reset();
        int parseIdx = 0;
        while (matcher.find()) {
          String flagStr = matcher.group(1);
          if (StringUtils.isEmpty(flagStr)) {
            continue;
          }
          if (configExplainReplaceMap.containsKey(flagStr)) {
            String fullFlag = "${" + flagStr + "}";
            int indexOfFullFlag = line.indexOf(fullFlag);
            lineNode.add(new Text(line.substring(parseIdx, indexOfFullFlag)));
            lineNode.add(configExplainReplaceMap.get(flagStr));
            parseIdx = indexOfFullFlag + fullFlag.length();
          }
        }
        if (parseIdx != line.length()) {
          Text text = new Text(line.substring(parseIdx));
          text.prefWidth(400);
          lineNode.add(text);
        }
        lineNode.add(new Text("\n"));
        textFlow.getChildren().addAll(lineNode);
      } else {
        textFlow.getChildren().add(new Text(line + "\n"));
      }
    }
  }

  @Override
  public void onCreate(Stage stage) {
    stage.setResizable(false);
  }

  @Override
  public String getTitle() {
    return "说明";
  }

  @Override
  public String getStageIconPath() {
    return "icon/describe.png";
  }

  @Override
  public String getFxmlPath() {
    return "describe";
  }
}
