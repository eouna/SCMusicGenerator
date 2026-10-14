package com.eouna.scmusicgenerator.generator.midi;

import com.eouna.scmusicgenerator.core.window.IWindowLogger;
import com.eouna.scmusicgenerator.generator.midi.ScMusicData.ScPage;
import com.eouna.scmusicgenerator.generator.midi.ScMusicData.ScPlate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.midi.*;

/**
 * 预播放: 用 Java 自带的软件合成器模拟游戏里的发音器电路, 让所有声部按时钟同时发声.
 *
 * <p>模拟规则(与游戏 {@code SoundGeneratorElectricElement} 一致):
 *
 * <ul>
 *   <li>电路时钟每个周期前进一格, 所有存储板同步输出该格的 P/O/V/乐器端数据
 *   <li>乐器端 0 -> 非 0 (上升沿) 且 P != F 且 V != 0 时发声; 恒定模式按"乐器端与时钟 AND"的接法理解, 即每格都有上升沿
 *   <li>同一块发音器两次发声间隔小于 0.08 秒时, 后一次被忽略
 *   <li>音高 = 乐器基准音 + 12 × O + P; 鼓按 P 选鼓声; 响度 = V / 15
 *   <li>每块发音器独立发声, 多个声部叠加播放
 * </ul>
 *
 * 游戏采样无法内置, 这里用 General MIDI 中音色相近的乐器代替, 用于检查节奏、和声与音域是否正确.
 *
 * @author CCL
 */
public class ScMusicPreviewPlayer implements AutoCloseable {

  /** 序列的时间分辨率: 一个存储格 = 一拍 = 96 tick */
  public static final int TICKS_PER_CELL = 96;

  /** MIDI 元事件: 速度 */
  private static final int META_SET_TEMPO = 0x51;

  /** MIDI 元事件: 标记 */
  private static final int META_MARKER = 0x06;

  /** MIDI 元事件: 音轨结束 */
  private static final int META_END_OF_TRACK = 0x2F;

  /** tempo 的最大值(3 字节) */
  private static final int MAX_MICROS_PER_BEAT = 0xFFFFFF;

  /** 乐曲结束后留给余音的时间(秒) */
  private static final double TAIL_SECONDS = 1.5;

  /** SC 乐器 -> 预览使用的 GM 音色 */
  private static final Map<ScInstrument, Integer> PREVIEW_PROGRAM = new EnumMap<>(ScInstrument.class);

  /** SC 乐器 -> MIDI 通道 */
  private static final Map<ScInstrument, Integer> PREVIEW_CHANNEL = new EnumMap<>(ScInstrument.class);

  /** SC 乐器 -> 一次发声的持续时间(秒), 模拟游戏采样的长度 */
  private static final Map<ScInstrument, Double> SOUND_SECONDS = new EnumMap<>(ScInstrument.class);

  /** SC 鼓声(P 端 0~9) -> GM 打击乐音符 */
  private static final int[] DRUM_TO_GM_NOTE = {38, 36, 42, 44, 46, 45, 50, 49, 51, 39};

  static {
    preview(ScInstrument.BELL, 0, 9, 1.5); // Glockenspiel
    preview(ScInstrument.ORGAN, 1, 19, 1.0); // Church Organ
    preview(ScInstrument.PING, 2, 11, 0.6); // Vibraphone
    preview(ScInstrument.STRING, 3, 48, 1.2); // String Ensemble
    preview(ScInstrument.TRUMPET, 4, 56, 1.0); // Trumpet
    preview(ScInstrument.VOICE, 5, 52, 1.2); // Choir Aahs
    preview(ScInstrument.PIANO, 6, 0, 1.2); // Acoustic Grand Piano
    preview(ScInstrument.PIANO_SUSTAIN, 7, 0, 3.0); // Acoustic Grand Piano, 更长的延音
    preview(ScInstrument.BASS, 8, 33, 1.0); // Electric Bass (finger)
    preview(ScInstrument.DRUMS, GmInstrumentMapping.PERCUSSION_CHANNEL, 0, 0.6); // Standard Kit
  }

  private static void preview(ScInstrument instrument, int channel, int program, double seconds) {
    PREVIEW_CHANNEL.put(instrument, channel);
    PREVIEW_PROGRAM.put(instrument, program);
    SOUND_SECONDS.put(instrument, seconds);
  }

  /** 一次发声 */
  private static final class Trigger {
    final long tick;
    final int order;
    final int channel;
    final int note;
    final int velocity;
    final long soundTicks;

    Trigger(long tick, int order, int channel, int note, int velocity, long soundTicks) {
      this.tick = tick;
      this.order = order;
      this.channel = channel;
      this.note = note;
      this.velocity = velocity;
      this.soundTicks = soundTicks;
    }
  }

  /** 电路模拟结果 */
  public static final class Simulation {
    /** 按时间排序的发声列表 */
    private final List<Trigger> triggers = new ArrayList<>();

    /** 因两次触发间隔小于 0.08 秒而被发音器忽略的音数 */
    private int skippedByRetrigger;

    public int getTriggerCount() {
      return triggers.size();
    }

    public int getSkippedByRetrigger() {
      return skippedByRetrigger;
    }
  }

  private Synthesizer synthesizer;
  private Sequencer sequencer;
  private MetaEventListener endListener;
  private final AtomicBoolean playing = new AtomicBoolean(false);
  private int totalCells;
  private double cellPeriodSeconds;

  /**
   * 模拟电路, 把存储板数据翻译成所有发音器的发声列表, 再写成 MIDI 序列(一格 = 一拍, 速度 = 时钟周期)
   *
   * @param data 音乐数据
   * @return 序列
   */
  public static Sequence buildSequence(ScMusicData data) throws InvalidMidiDataException {
    return buildSequence(data, simulateCircuit(data));
  }

  /**
   * 把模拟结果写成 MIDI 序列
   *
   * @param data 音乐数据
   * @param simulation 电路模拟结果
   * @return 序列
   */
  public static Sequence buildSequence(ScMusicData data, Simulation simulation)
      throws InvalidMidiDataException {
    Objects.requireNonNull(data);
    Sequence sequence = new Sequence(Sequence.PPQ, TICKS_PER_CELL);
    Track track = sequence.createTrack();

    // 速度: 一拍(一格) = 一个时钟周期
    int microsPerCell =
        (int)
            Math.min(
                MAX_MICROS_PER_BEAT,
                Math.max(1, Math.round(data.getCellPeriodSeconds() * 1_000_000)));
    track.add(new MidiEvent(tempoMessage(microsPerCell), 0));

    // 音色: 每种 SC 乐器一个通道
    for (ScInstrument instrument : data.getPlates().keySet()) {
      int channel = PREVIEW_CHANNEL.get(instrument);
      track.add(
          new MidiEvent(
              new ShortMessage(
                  ShortMessage.PROGRAM_CHANGE, channel, PREVIEW_PROGRAM.get(instrument), 0),
              0));
      // 通道音量拉满, 响度完全由 V 端决定
      track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 7, 127), 0));
    }

    List<Trigger> triggers = simulation.triggers;
    long lastTick = 0;
    // 还没写入的松键事件: (通道, 音符) -> 事件. 同一音符再次触发时先把上一次的松键提前, 避免新音被旧的松键切断
    Map<Integer, MidiEvent> pendingNoteOff = new HashMap<>();
    for (Trigger trigger : triggers) {
      int key = trigger.channel * 128 + trigger.note;
      MidiEvent previousOff = pendingNoteOff.remove(key);
      if (previousOff != null) {
        previousOff.setTick(Math.min(previousOff.getTick(), trigger.tick));
        track.add(previousOff);
      }
      track.add(
          new MidiEvent(
              new ShortMessage(ShortMessage.NOTE_ON, trigger.channel, trigger.note, trigger.velocity),
              trigger.tick));
      MidiEvent noteOff =
          new MidiEvent(
              new ShortMessage(ShortMessage.NOTE_OFF, trigger.channel, trigger.note, 0),
              trigger.tick + trigger.soundTicks);
      pendingNoteOff.put(key, noteOff);
      lastTick = Math.max(lastTick, trigger.tick + trigger.soundTicks);
    }
    for (MidiEvent event : pendingNoteOff.values()) {
      track.add(event);
    }
    // 留出余音
    long tailTicks = Math.round(TAIL_SECONDS / data.getCellPeriodSeconds() * TICKS_PER_CELL);
    track.add(new MidiEvent(new MetaMessage(META_MARKER, new byte[0], 0), lastTick + tailTicks));
    return sequence;
  }

  /**
   * 逐格模拟每一块发音器, 得到按时间排序的发声列表
   *
   * @param data 音乐数据
   * @return 模拟结果
   */
  public static Simulation simulateCircuit(ScMusicData data) {
    Simulation simulation = new Simulation();
    List<Trigger> triggers = simulation.triggers;
    int cellsPerPage = data.getStepsPerPage() * data.getToneMode().getCellsPerStep();
    double cellPeriod = data.getCellPeriodSeconds();
    int order = 0;
    for (ScPlate plate : data.getAllPlates()) {
      ScInstrument instrument = plate.instrument;
      int channel = PREVIEW_CHANNEL.get(instrument);
      long soundTicks =
          Math.max(1, Math.round(SOUND_SECONDS.get(instrument) / cellPeriod * TICKS_PER_CELL));
      char previousTone = '0';
      long lastPlayedCell = Long.MIN_VALUE;
      for (ScPage page : plate.pages) {
        for (int i = 0; i < page.pitchData.length(); i++) {
          long cell = (long) page.pageIndex * cellsPerPage + i;
          char tone = page.toneData.charAt(i);
          char pitchChar = page.pitchData.charAt(i);
          boolean risingEdge =
              tone != '0'
                  && (previousTone == '0' || data.getToneMode() == ToneMode.CONSTANT);
          previousTone = tone;
          if (!risingEdge || pitchChar == 'F') {
            continue;
          }
          int pitch = Character.digit(pitchChar, 16);
          int octave = Character.digit(page.octaveData.charAt(i), 16);
          int volume = Character.digit(page.volumeData.charAt(i), 16);
          if (pitch < 0 || octave < 0 || volume <= 0) {
            continue;
          }
          // 游戏限制: 同一发音器两次发声间隔 >= 0.08s
          if (lastPlayedCell != Long.MIN_VALUE
              && (cell - lastPlayedCell) * cellPeriod
                  < ScMusicDataBuilder.MIN_RETRIGGER_SECONDS - 1e-9) {
            simulation.skippedByRetrigger++;
            continue;
          }
          int midiNote = toMidiNote(instrument, pitch, octave);
          if (midiNote < 0) {
            continue;
          }
          lastPlayedCell = cell;
          int velocity = Math.max(1, Math.min(127, (int) Math.round(volume / 15.0 * 127)));
          triggers.add(
              new Trigger(cell * TICKS_PER_CELL, order++, channel, midiNote, velocity, soundTicks));
        }
      }
    }
    triggers.sort(Comparator.comparingLong((Trigger t) -> t.tick).thenComparingInt(t -> t.order));
    return simulation;
  }

  /**
   * 发音器 (P, O) 实际发出的 MIDI 音符
   *
   * @return 音符, 无法表达时返回 -1
   */
  static int toMidiNote(ScInstrument instrument, int pitch, int octave) {
    if (instrument.isDrums()) {
      return pitch < DRUM_TO_GM_NOTE.length ? DRUM_TO_GM_NOTE[pitch] : -1;
    }
    int note = instrument.getLowestMidiNote() + 12 * octave + pitch;
    return Math.max(0, Math.min(127, note));
  }

  private static MetaMessage tempoMessage(int microsPerBeat) throws InvalidMidiDataException {
    byte[] data = {
      (byte) ((microsPerBeat >> 16) & 0xFF),
      (byte) ((microsPerBeat >> 8) & 0xFF),
      (byte) (microsPerBeat & 0xFF)
    };
    return new MetaMessage(META_SET_TEMPO, data, data.length);
  }

  /**
   * 开始播放. 第一次调用需要初始化软件合成器(可能要几秒), 建议在后台线程调用
   *
   * @param logger 日志
   * @param data 音乐数据
   * @param onFinished 自然播放结束时的回调(在 MIDI 线程中调用), 可为 null
   */
  public synchronized void play(IWindowLogger logger, ScMusicData data, Runnable onFinished)
      throws MidiUnavailableException, InvalidMidiDataException {
    stop();
    Simulation simulation = simulateCircuit(data);
    Sequence sequence = buildSequence(data, simulation);
    cellPeriodSeconds = data.getCellPeriodSeconds();
    totalCells = data.getTotalSteps() * data.getToneMode().getCellsPerStep();
    ensureDevicesOpen(logger);
    sequencer.setSequence(sequence);
    sequencer.setTempoFactor(1.0f);
    sequencer.setTickPosition(0);
    endListener =
        meta -> {
          if (meta.getType() == META_END_OF_TRACK) {
            playing.set(false);
            if (onFinished != null) {
              onFinished.run();
            }
          }
        };
    sequencer.addMetaEventListener(endListener);
    playing.set(true);
    sequencer.start();
    logger.info(
        "预播放开始: {} 个声部同时播放, 共 {} 格, 每格 {} 秒, 发声 {} 次, 约 {} 秒",
        data.getPlateCount(),
        totalCells,
        String.format("%.4f", cellPeriodSeconds),
        simulation.getTriggerCount(),
        String.format("%.1f", sequence.getMicrosecondLength() / 1_000_000.0));
    if (simulation.getSkippedByRetrigger() > 0) {
      logger.warn(
          "有 {} 个音因同一发音器两次触发间隔小于 0.08 秒被忽略(游戏中同样会丢), 建议换更大的步长",
          simulation.getSkippedByRetrigger());
    }
  }

  private void ensureDevicesOpen(IWindowLogger logger) throws MidiUnavailableException {
    if (sequencer != null && sequencer.isOpen()) {
      return;
    }
    close();
    try {
      synthesizer = MidiSystem.getSynthesizer();
      synthesizer.open();
      sequencer = MidiSystem.getSequencer(false);
      sequencer.open();
      sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
      Soundbank soundbank = synthesizer.getDefaultSoundbank();
      logger.debug(
          "合成器: {} , 音色库: {}",
          synthesizer.getDeviceInfo().getName(),
          soundbank == null ? "无" : soundbank.getName());
    } catch (MidiUnavailableException e) {
      // 没有可用的软件合成器时退回系统默认 MIDI 设备
      close();
      sequencer = MidiSystem.getSequencer(true);
      sequencer.open();
    }
  }

  /** 停止播放(保留已打开的合成器, 下次播放不用再初始化) */
  public synchronized void stop() {
    playing.set(false);
    if (sequencer == null) {
      return;
    }
    if (endListener != null) {
      sequencer.removeMetaEventListener(endListener);
      endListener = null;
    }
    if (sequencer.isOpen() && sequencer.isRunning()) {
      sequencer.stop();
    }
    allNotesOff();
  }

  private void allNotesOff() {
    if (synthesizer == null || !synthesizer.isOpen()) {
      return;
    }
    for (MidiChannel channel : synthesizer.getChannels()) {
      if (channel != null) {
        channel.allNotesOff();
        channel.allSoundOff();
      }
    }
  }

  public boolean isPlaying() {
    return playing.get();
  }

  /** 当前播放到第几格(从 0 开始) */
  public long getCurrentCell() {
    if (sequencer == null || !sequencer.isOpen()) {
      return 0;
    }
    return sequencer.getTickPosition() / TICKS_PER_CELL;
  }

  /** 当前播放到的秒数 */
  public double getCurrentSeconds() {
    if (sequencer == null || !sequencer.isOpen()) {
      return 0;
    }
    return sequencer.getMicrosecondPosition() / 1_000_000.0;
  }

  public int getTotalCells() {
    return totalCells;
  }

  public double getCellPeriodSeconds() {
    return cellPeriodSeconds;
  }

  /** 释放合成器与音序器 */
  @Override
  public synchronized void close() {
    playing.set(false);
    endListener = null;
    if (sequencer != null) {
      try {
        if (sequencer.isOpen()) {
          if (sequencer.isRunning()) {
            sequencer.stop();
          }
          sequencer.close();
        }
      } catch (Exception ignored) {
      }
      sequencer = null;
    }
    if (synthesizer != null) {
      try {
        if (synthesizer.isOpen()) {
          synthesizer.close();
        }
      } catch (Exception ignored) {
      }
      synthesizer = null;
    }
  }
}
