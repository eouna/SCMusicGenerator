package com.eouna.scmusicgenerator.generator.midi;

import com.eouna.scmusicgenerator.core.window.IWindowLogger;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.sound.midi.*;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * 解码 MIDI 文件: 把所有轨道按时间合并, 按通道跟踪音色, 得到映射到 SC 乐器之后的按键列表.
 *
 * <p>要点:
 *
 * <ul>
 *   <li>Program Change 作用于通道而不是轨道, 所以所有轨道的事件先按 tick 合并, 再用 16 个通道各自的当前音色解析
 *   <li>通道 10(索引 9) 永远是鼓, 与 Program Change 无关
 *   <li>NOTE_OFF 与 velocity=0 的 NOTE_ON 都只是松键, 对 SC 发音器没有意义, 不产生数据
 * </ul>
 *
 * @author CCL
 */
public class MidiFileDecoder {

  /** 速度表 */
  private MidiTempoMap tempoMap;

  /** 映射成功的按键, 按 tick 升序 */
  private final List<MidiNote> notes = new ArrayList<>();

  /** 按 SC 乐器分组的按键 */
  private final Map<ScInstrument, List<MidiNote>> notesByInstrument = new TreeMap<>();

  /** 不支持的 GM 音色(0 基) */
  private final Set<Integer> unsupportedPrograms = new TreeSet<>();

  /** 因不支持的音色而丢弃的按键数 */
  private int droppedNoteCount;

  /** 出现过按键的通道 */
  private final Set<Integer> usedChannels = new TreeSet<>();

  /** 最后一个按键的 tick */
  private long lastNoteTick;

  /** 时序分辨率 */
  private int resolution;

  /**
   * 解码 MIDI 文件
   *
   * @param logger logger
   * @param midiFile midi文件
   */
  public void decode(IWindowLogger logger, File midiFile) {
    String lowerName = midiFile.getName().toLowerCase(Locale.ROOT);
    if (!lowerName.endsWith(".mid") && !lowerName.endsWith(".midi")) {
      logger.error("文件选择错误");
      throw new RuntimeException("请选择MIDI文件");
    }
    Sequence sequence = getMidiSequence(logger, midiFile);
    showMidiInfo(logger, sequence);
    decodeSequence(logger, sequence);
    logger.info("数据解析完成");
  }

  private Sequence getMidiSequence(IWindowLogger logger, File midiFile) {
    try {
      return MidiSystem.getSequence(midiFile);
    } catch (InvalidMidiDataException e) {
      logger.error("MIDI音乐选择错误");
      throw new RuntimeException(e);
    } catch (IOException e) {
      if (e instanceof EOFException) {
        logger.error("错误的MIDI文件");
      } else {
        logger.error("IO异常", e);
      }
      throw new RuntimeException(e);
    }
  }

  private void showMidiInfo(IWindowLogger logger, Sequence sequence) {
    boolean ppq = sequence.getDivisionType() == Sequence.PPQ;
    String info =
        "MIDI文件信息:\n时序分辨率: "
            + sequence.getResolution()
            + (ppq ? " tick/四分音符" : " tick/帧")
            + "\n计时方式: "
            + (ppq ? "PPQ(按拍)" : "SMPTE " + sequence.getDivisionType() + " 帧/秒")
            + "\n总时长: "
            + DurationFormatUtils.formatDurationWords(
                sequence.getMicrosecondLength() / 1000, true, true)
            + "\n总tick: "
            + sequence.getTickLength()
            + "\nTrack数: "
            + sequence.getTracks().length;
    logger.info(info);
  }

  /** 一个带来源信息的事件, 用于把多个轨道按时间稳定排序 */
  private static final class OrderedEvent {
    final long tick;
    final int trackIndex;
    final int eventIndex;
    final MidiMessage message;

    OrderedEvent(long tick, int trackIndex, int eventIndex, MidiMessage message) {
      this.tick = tick;
      this.trackIndex = trackIndex;
      this.eventIndex = eventIndex;
      this.message = message;
    }
  }

  /**
   * 解码 midi 序列
   *
   * @param sequence 音乐序列
   */
  private void decodeSequence(IWindowLogger logger, Sequence sequence) {
    notes.clear();
    notesByInstrument.clear();
    unsupportedPrograms.clear();
    usedChannels.clear();
    droppedNoteCount = 0;
    lastNoteTick = 0;
    resolution = sequence.getResolution();
    tempoMap = new MidiTempoMap(sequence);

    // 1. 合并所有轨道的事件, 按 tick 排序(同 tick 保持轨道顺序/文件顺序)
    List<OrderedEvent> events = new ArrayList<>();
    Track[] tracks = sequence.getTracks();
    for (int trackIndex = 0; trackIndex < tracks.length; trackIndex++) {
      Track track = tracks[trackIndex];
      for (int eventIndex = 0; eventIndex < track.size(); eventIndex++) {
        MidiEvent midiEvent = track.get(eventIndex);
        events.add(
            new OrderedEvent(midiEvent.getTick(), trackIndex, eventIndex, midiEvent.getMessage()));
      }
    }
    events.sort(
        Comparator.comparingLong((OrderedEvent e) -> e.tick)
            .thenComparingInt(e -> e.trackIndex)
            .thenComparingInt(e -> e.eventIndex));

    // 2. 按通道跟踪音色, 解析按键
    int[] channelProgram = new int[16];
    int noteOnCount = 0;
    int noteOffCount = 0;
    int programChangeCount = 0;
    for (OrderedEvent event : events) {
      MidiMessage message = event.message;
      if (message instanceof MetaMessage) {
        MetaMessage meta = (MetaMessage) message;
        // FF 51 03 tt tt tt : Set Tempo
        if (meta.getType() == 0x51 && meta.getData().length >= 3) {
          byte[] data = meta.getData();
          int microsPerBeat =
              ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
          tempoMap.addTempoChange(event.tick, microsPerBeat);
        }
        continue;
      }
      if (!(message instanceof ShortMessage)) {
        continue;
      }
      ShortMessage shortMessage = (ShortMessage) message;
      int channel = shortMessage.getChannel();
      switch (shortMessage.getCommand()) {
        case ShortMessage.PROGRAM_CHANGE:
          // 音色是通道的状态, 与轨道无关; 是否支持要等真正有按键时再判断
          channelProgram[channel] = shortMessage.getData1();
          programChangeCount++;
          break;
        case ShortMessage.NOTE_ON:
          if (shortMessage.getData2() <= 0) {
            // velocity 0 的 NOTE_ON 等价于 NOTE_OFF
            noteOffCount++;
            break;
          }
          noteOnCount++;
          usedChannels.add(channel);
          handleNoteOn(event.tick, channel, channelProgram[channel], shortMessage);
          break;
        case ShortMessage.NOTE_OFF:
          noteOffCount++;
          break;
        default:
          break;
      }
    }
    tempoMap.build();

    // 3. 汇总
    logger.info(
        "歌曲的通道数量: {}, 音色切换: {} 次, 按键: {} 次, 松键: {} 次",
        usedChannels.size(),
        programChangeCount,
        noteOnCount,
        noteOffCount);
    double initialBpm = MidiTempoMap.microsPerBeatToBpm(tempoMap.getInitialMicrosPerBeat());
    if (tempoMap.getDistinctTempoCount() > 1) {
      logger.warn(
          "MIDI 中有 {} 种不同速度(起始 {} BPM), 游戏电路时钟无法跟随变速, 将按持续时间最长的速度计算时钟周期",
          tempoMap.getDistinctTempoCount(),
          String.format("%.1f", initialBpm));
    } else {
      logger.info("速度: {} BPM", String.format("%.1f", initialBpm));
    }
    if (!unsupportedPrograms.isEmpty()) {
      logger.warn(
          "发现不支持的乐器(音效类), 对应音符已丢弃 {} 个: \n{}",
          droppedNoteCount,
          unsupportedPrograms.stream()
              .map(p -> p + " " + InstrumentInfo.getInstrumentNameByProgram(p))
              .collect(Collectors.joining("\n")));
    }
    for (Map.Entry<ScInstrument, List<MidiNote>> entry : notesByInstrument.entrySet()) {
      List<MidiNote> list = entry.getValue();
      Set<String> sources =
          list.stream()
              .map(
                  n ->
                      n.channel == GmInstrumentMapping.PERCUSSION_CHANNEL
                          ? "通道10(鼓)"
                          : InstrumentInfo.getInstrumentNameByProgram(n.program))
              .collect(Collectors.toCollection(LinkedHashSet::new));
      logger.info(
          "SC乐器 [{}] <= {} , 音符 {} 个, 音域 {} ~ {}",
          entry.getKey(),
          String.join(" / ", sources),
          list.size(),
          InstrumentInfo.midiNoteName(list.stream().mapToInt(n -> n.note).min().orElse(0)),
          InstrumentInfo.midiNoteName(list.stream().mapToInt(n -> n.note).max().orElse(0)));
    }
  }

  private void handleNoteOn(long tick, int channel, int program, ShortMessage shortMessage) {
    ScInstrument instrument = GmInstrumentMapping.instrumentOf(channel, program);
    if (instrument == null) {
      droppedNoteCount++;
      unsupportedPrograms.add(program);
      return;
    }
    MidiNote midiNote =
        new MidiNote(
            tick, channel, program, shortMessage.getData1(), shortMessage.getData2(), instrument);
    notes.add(midiNote);
    notesByInstrument.computeIfAbsent(instrument, k -> new ArrayList<>()).add(midiNote);
    lastNoteTick = Math.max(lastNoteTick, tick);
  }

  public MidiTempoMap getTempoMap() {
    return tempoMap;
  }

  /** 全部按键, 按 tick 升序 */
  public List<MidiNote> getNotes() {
    return notes;
  }

  /** 按 SC 乐器分组的按键(乐器码升序) */
  public Map<ScInstrument, List<MidiNote>> getNotesByInstrument() {
    return notesByInstrument;
  }

  public Set<Integer> getUnsupportedPrograms() {
    return unsupportedPrograms;
  }

  public int getDroppedNoteCount() {
    return droppedNoteCount;
  }

  public long getLastNoteTick() {
    return lastNoteTick;
  }

  public int getResolution() {
    return resolution;
  }

  /** 需要用到的 SC 乐器种类数 */
  public int getInstrumentNeededNum() {
    return notesByInstrument.size();
  }
}
