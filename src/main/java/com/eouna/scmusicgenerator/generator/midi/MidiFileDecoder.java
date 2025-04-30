package com.eouna.scmusicgenerator.generator.midi;

import com.eouna.scmusicgenerator.generator.midi.NoteDataCollection.ScNoteData;
import com.eouna.scmusicgenerator.utils.MainWindowTextAreaLogger;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.sound.midi.*;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * 解码MIDI文件
 *
 * @author CCL
 */
public class MidiFileDecoder {

  /** 保存解析好的MIDI音乐数据, 音乐通道 <=>{tick,音乐数据} */
  private final Map<Integer, NoteDataCollection> scNoteData = new HashMap<>();

  /** MIDI乐器编号和SC乐器映射 */
  private static final Map<Integer, Integer> GM_TO_CUSTOM = new HashMap<>();

  /** 最大页数 */
  private int showPanelMaxPage;

  /** 时序分辨率 */
  private int resolution;

  /** 生效能用的乐器数量 */
  private int instrumentNeededNum;

  static {
    // GM Program Number (0-127) to Custom InstrumentC ode )(1-10)
    // 钟铃类（Bell-like）
    // 木偶报时钟
    GM_TO_CUSTOM.put(9, 0x1);
    // 音乐盒
    GM_TO_CUSTOM.put(10, 0x1);
    // 颤音琴
    GM_TO_CUSTOM.put(11, 0x1);

    // 风琴类（Organ）
    // 电风琴
    GM_TO_CUSTOM.put(16, 0x2);
    // 打击风琴
    GM_TO_CUSTOM.put(17, 0x2);
    // 摇滚风琴
    GM_TO_CUSTOM.put(18, 0x2);
    // 教堂风琴
    GM_TO_CUSTOM.put(19, 0x2);
    // 簧风琴
    GM_TO_CUSTOM.put(20, 0x2);
    // 手风琴
    GM_TO_CUSTOM.put(21, 0x2);

    // 砰类（Synth Effects / Drum-like）
    // FX 1 (rain)
    GM_TO_CUSTOM.put(96, 0x3);
    // FX 2 (soundtrack)
    GM_TO_CUSTOM.put(97, 0x3);
    // FX 3 (crystal)
    GM_TO_CUSTOM.put(98, 0x3);

    // 弦乐类（Strings）
    // 小提琴
    GM_TO_CUSTOM.put(40, 0x4);
    // 中提琴
    GM_TO_CUSTOM.put(41, 0x4);
    // 大提琴
    GM_TO_CUSTOM.put(42, 0x4);
    // 低音提琴
    GM_TO_CUSTOM.put(43, 0x4);
    // 颤音弦乐
    GM_TO_CUSTOM.put(44, 0x4);
    // 拨奏弦乐
    GM_TO_CUSTOM.put(45, 0x4);
    // 竖琴
    GM_TO_CUSTOM.put(46, 0x4);

    // 小号类（Brass）
    // 小号
    GM_TO_CUSTOM.put(56, 0x5);
    // 长号
    GM_TO_CUSTOM.put(57, 0x5);
    // 大号
    GM_TO_CUSTOM.put(58, 0x5);
    // 闷音小号
    GM_TO_CUSTOM.put(59, 0x5);

    // 语音（Voice/Synth Voice）
    // 人声
    GM_TO_CUSTOM.put(54, 0x6);
    // 合成人声
    GM_TO_CUSTOM.put(55, 0x6);

    // 钢琴（Piano）
    // 大钢琴
    GM_TO_CUSTOM.put(0, 0x7);
    // 明亮原声钢琴
    GM_TO_CUSTOM.put(1, 0x7);
    // 电钢琴
    GM_TO_CUSTOM.put(2, 0x7);
    // 叮当琴
    GM_TO_CUSTOM.put(3, 0x7);
    // 电子琴1
    GM_TO_CUSTOM.put(4, 0x7);
    // 电子琴2
    GM_TO_CUSTOM.put(5, 0x7);

    // 钢琴延音（Sustain Piano）
    // 大键琴
    GM_TO_CUSTOM.put(6, 0x8);
    // 古钢琴
    GM_TO_CUSTOM.put(7, 0x8);

    // 贝斯（Bass）
    // 原声贝司
    GM_TO_CUSTOM.put(32, 0xA);
    // 电贝司（手指）
    GM_TO_CUSTOM.put(33, 0xA);
    // 电贝司（拨片）
    GM_TO_CUSTOM.put(34, 0xA);
    // 无品贝司
    GM_TO_CUSTOM.put(35, 0xA);

    // 鼓通道（Channel 9）特殊标记为 -1
    // 鼓
    GM_TO_CUSTOM.put(-1, 0x9);
  }

  /**
   * 解码MIDI文件
   *
   * @param textAreaLogger logger
   * @param midiFile midi文件
   */
  public void decode(MainWindowTextAreaLogger textAreaLogger, File midiFile) {
    if (!midiFile.getName().endsWith(".mid")) {
      textAreaLogger.error("文件选择错误");
      throw new RuntimeException("请选择MIDI文件");
    }
    // 获取序列
    Sequence sequence = getMidiSequence(textAreaLogger, midiFile);
    if (sequence == null) {
      textAreaLogger.warn("获取MIDI数据异常");
      return;
    }
    // 显示文件信息
    showMidiInfo(textAreaLogger, sequence);
    // 解析序列
    decodeSequence(textAreaLogger, sequence);
    textAreaLogger.info("数据解析完成");
    instrumentNeededNum = scNoteData.size();
    // 保存时序分辨率
    resolution = sequence.getResolution();
  }

  private Sequence getMidiSequence(MainWindowTextAreaLogger textAreaLogger, File midiFile) {
    Sequence sequence;
    try {
      sequence = MidiSystem.getSequence(midiFile);
    } catch (InvalidMidiDataException e) {
      textAreaLogger.error("MIDI音乐选择错误");
      throw new RuntimeException(e);
    } catch (IOException e) {
      if (e instanceof EOFException) {
        textAreaLogger.error("错误的MIDI文件");
      } else {
        textAreaLogger.error("IO异常", e);
      }
      throw new RuntimeException(e);
    }
    return sequence;
  }

  private void showMidiInfo(MainWindowTextAreaLogger textAreaLogger, Sequence sequence) {
    String info =
        "MIDI文件信息:\nMIDI时序分辨率: "
            + sequence.getResolution()
            + "\n节拍划分类型: "
            + sequence.getDivisionType()
            + "\n总时长: "
            + DurationFormatUtils.formatDurationWords(
                sequence.getMicrosecondLength() / 1000, true, true)
            + "\n节拍长度: "
            + sequence.getTickLength()
            + "\n Track数："
            + sequence.getTracks().length;
    textAreaLogger.info(info);
  }

  /**
   * 解码midi数据
   *
   * @param sequence 音乐序列
   */
  private void decodeSequence(MainWindowTextAreaLogger logger, Sequence sequence) {
    if (sequence == null) {
      return;
    }
    Track[] tracks = sequence.getTracks();
    HashSet<Integer> notSupportedInstrument = new HashSet<>();
    HashSet<Integer> channelNum = new HashSet<>();
    for (Track track : tracks) {
      int instrumentCode = 0;
      for (int trackEventPointer = 0; trackEventPointer < track.size(); trackEventPointer++) {
        MidiEvent midiEvent = track.get(trackEventPointer);
        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
          ShortMessage shortMidiMessage = (ShortMessage) midiMessage;
          channelNum.add(shortMidiMessage.getChannel());
          // 获取消息指令
          switch (shortMidiMessage.getCommand()) {
            case ShortMessage.NOTE_ON:
              // 如果最开始就没有 PROGRAM_CHANGE 事件则直接设置为默认乐器
              if (!scNoteData.containsKey(instrumentCode)
                  && GM_TO_CUSTOM.containsKey(instrumentCode)) {
                scNoteData.put(
                    instrumentCode, new NoteDataCollection(shortMidiMessage.getChannel()));
              }
              handMidiMessageNoteOn(shortMidiMessage, midiEvent.getTick(), instrumentCode);
              break;
            case ShortMessage.PROGRAM_CHANGE:
              instrumentCode =
                  handMidiMessageProgramChange(shortMidiMessage, notSupportedInstrument);
              break;
            case ShortMessage.NOTE_OFF:
              handMidiMessageNoteOff(instrumentCode, midiEvent.getTick());
              break;
            default:
              break;
          }
        }
      }
    }
    if (!notSupportedInstrument.isEmpty()) {
      logger.warn(
          "发现不支持的乐器: \n{}",
          notSupportedInstrument.stream()
              .map(InstrumentInfo::getInstrumentNameByCode)
              .collect(Collectors.joining("\n")));
    }
    logger.info("歌曲的通道数量：{}", channelNum.size());
  }

  /**
   * 处理midi的note on事件(按下音符)
   *
   * @param shortMessage 当前midi信息
   * @param tick tick
   */
  private void handMidiMessageNoteOn(ShortMessage shortMessage, long tick, int instrumentCode) {
    if (!GM_TO_CUSTOM.containsKey(instrumentCode)) {
      return;
    }
    int note = shortMessage.getData1();
    int velocity = shortMessage.getData2();
    ScNoteData noteData = new ScNoteData();
    int currentInstrument = GM_TO_CUSTOM.get(instrumentCode);
    // 初始化后再写入数据
    NoteDataCollection noteDataCollection = this.scNoteData.get(instrumentCode);
    // note off
    if (velocity <= 0) {
      return;
    }
    // 音量映射: 0-127 -> 1-15 (0 最轻, F(15) 最响)
    int analogVel = (int) Math.ceil(velocity / 127.0 * 15);
    if (analogVel < 1) {
      analogVel = 1;
    }
    if (analogVel > 15) {
      analogVel = 15;
    }
    // 当前通道不为鼓时
    if (currentInstrument != 9) {
      int baseOct = (int) Math.floor(note / 12.0);
      // 非鼓：音高映射到 0~14
      int m = note % 12;
      int pitch;
      if (m <= 2) {
        // C, C# 或 D 音符，映射到下一八度的音高
        pitch = m + 12;
      } else {
        // 其它音符，直接映射
        pitch = m;
      }
      // 更新当前八度最大最小
      noteDataCollection.updateNoteInfoData(baseOct, analogVel);
      noteData.pitch = pitch;
      noteData.octaves = baseOct;
    } else {
      noteData.pitch = note % 10;
      noteData.octaves = 0;
    }
    noteData.velocity = analogVel;
    TreeMap<Long, List<ScNoteData>> scNoteDataMap = noteDataCollection.getNoteData();
    scNoteDataMap.computeIfAbsent(tick, k -> new ArrayList<>()).add(noteData);
    noteDataCollection.updateChannel(shortMessage.getChannel());
  }

  /**
   * 处理midi的note on事件(按下音符)通过平均八度来处理
   *
   * @param shortMessage 当前midi信息
   * @param tick tick
   */
  private void handMidiMessageNoteOnByAverageOctave(
      ShortMessage shortMessage, long tick, int instrumentCode) {
    if (!GM_TO_CUSTOM.containsKey(instrumentCode)) {
      return;
    }
    int note = shortMessage.getData1();
    int velocity = shortMessage.getData2();
    ScNoteData noteData = new ScNoteData();
    int currentInstrument = GM_TO_CUSTOM.get(instrumentCode);
    // 初始化后再写入数据
    NoteDataCollection noteDataCollection = this.scNoteData.get(instrumentCode);
    // note off
    if (velocity <= 0) {
      return;
    }
    // 当前通道不为鼓时
    if (currentInstrument != 9) {
      // 非鼓：音高映射到 0~14
      int m = note % 12;
      int baseOct = (int) Math.floor(note / 12.0);
      int pitch;
      int octave = baseOct;
      if (m <= 2) {
        // C, C# 或 D 音符，映射到下一八度的音高
        pitch = m + 12;
        // octave = baseOct - 1;
      } else {
        // 其它音符，直接映射
        pitch = m;
      }
      // 限制八度范围,平均将八度下调
      if (currentInstrument == 7 || currentInstrument == 8) {
        if (pitch >= 12) {
          octave = octave < 4 && octave >= 1 ? 1 : (octave < 7 ? 2 : 3);
        } else {
          octave = octave <= 3 && octave > 0 ? 1 : (octave <= 6 ? 2 : 3);
        }
      } else {
        // 其它非鼓乐器，八度范围 0~2
        octave = Math.max(0, Math.min(octave, 2));
      }
      noteData.pitch = pitch;
      noteData.octaves = octave;
    } else {
      noteData.pitch = note % 10;
      noteData.octaves = 0;
    }
    // 音量映射: 0-127 -> 1-15 (0 最轻, F(15) 最响)
    int analogVel = (int) Math.ceil(velocity / 127.0 * 15);
    if (analogVel < 1) {
      analogVel = 1;
    }
    if (analogVel > 15) {
      analogVel = 15;
    }
    noteData.velocity = analogVel;
    TreeMap<Long, List<ScNoteData>> scNoteDataMap = noteDataCollection.getNoteData();
    scNoteDataMap.computeIfAbsent(tick, k -> new ArrayList<>()).add(noteData);
  }

  /** 处理乐器改变事件 */
  private int handMidiMessageProgramChange(
      ShortMessage shortMidiMessage, HashSet<Integer> notSupportedInstrument) {
    int originInstrument = shortMidiMessage.getData1();
    int currentInstrument = GM_TO_CUSTOM.getOrDefault(originInstrument, 0);
    if (shortMidiMessage.getChannel() == 9) {
      originInstrument = -1;
    }
    if (currentInstrument != 0) {
      // 初始化当前通道数据
      scNoteData.putIfAbsent(
          originInstrument, new NoteDataCollection(shortMidiMessage.getChannel()));
    } else {
      // 不支持的乐器
      notSupportedInstrument.add(shortMidiMessage.getData1());
    }
    return originInstrument;
  }

  private void handMidiMessageNoteOff(int currentInstrument, long tick) {
    NoteDataCollection noteOfTickData = this.scNoteData.get(currentInstrument);
    if (noteOfTickData == null) {
      return;
    }
    ScNoteData noteData = new ScNoteData();
    noteData.pitch = 15;
    noteData.octaves = 0;
    noteData.velocity = 0;

    noteOfTickData.getNoteData().computeIfAbsent(tick, k -> new ArrayList<>()).add(noteData);
  }

  public Map<Integer, NoteDataCollection> getScNoteData() {
    return scNoteData;
  }

  public void setShowPanelMaxPage(int showPanelMaxPage) {
    this.showPanelMaxPage = showPanelMaxPage;
  }

  public int getShowPanelMaxPage() {
    return showPanelMaxPage;
  }

  public int getResolution() {
    return resolution;
  }

  public int getInstrumentNeededNum() {
    return instrumentNeededNum;
  }

  public static Map<Integer, Integer> getGmToCustom() {
    return GM_TO_CUSTOM;
  }
}
