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
        "【功能说明】: 将.mid文件和.mp3文件解析成生存战争中可用的音乐板数据\n\n"
            + "【使用方法】: 直接选择文件后点击生成生存战争中的音乐数据，然后将生成的数据复制到音乐模块对应M板上\n\n"
            + "【数据说明】: P端(0–E), O端(0-3), V端(0-F)\n\n"
            + "【参考资料】: \n"
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
