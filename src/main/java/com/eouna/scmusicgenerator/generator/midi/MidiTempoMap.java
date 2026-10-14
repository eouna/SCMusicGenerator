package com.eouna.scmusicgenerator.generator.midi;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.sound.midi.Sequence;

/**
 * MIDI 速度表: 负责 tick 与实际时间之间的换算.
 *
 * <ul>
 *   <li>PPQ 文件: resolution = 每个四分音符的 tick 数, 时间由 Set Tempo(FF 51) 元事件决定, 默认 120 BPM
 *   <li>SMPTE 文件: tick 本身就是时间, 每秒 tick 数 = 帧率 * 每帧 tick 数
 * </ul>
 *
 * @author CCL
 */
public class MidiTempoMap {

  /** MIDI 规范默认速度: 500000 微秒一拍 = 120 BPM */
  public static final int DEFAULT_MICROS_PER_BEAT = 500000;

  private final float divisionType;
  private final int resolution;

  /** tick -> 从该 tick 起的速度(微秒/拍) */
  private final TreeMap<Long, Integer> tempoChanges = new TreeMap<>();

  /** 与 tempoChanges 对应的累计微秒 */
  private final TreeMap<Long, Long> microsAtTempoChange = new TreeMap<>();

  public MidiTempoMap(Sequence sequence) {
    this.divisionType = sequence.getDivisionType();
    this.resolution = sequence.getResolution();
    tempoChanges.put(0L, DEFAULT_MICROS_PER_BEAT);
  }

  /** 是否为 PPQ(按拍)计时 */
  public boolean isPpq() {
    return divisionType == Sequence.PPQ;
  }

  public int getResolution() {
    return resolution;
  }

  /**
   * 记录一次速度变化
   *
   * @param tick 发生 tick
   * @param microsPerBeat 微秒/拍
   */
  public void addTempoChange(long tick, int microsPerBeat) {
    if (microsPerBeat <= 0) {
      return;
    }
    tempoChanges.put(Math.max(0, tick), microsPerBeat);
    // 累计时间需要重新计算
    microsAtTempoChange.clear();
  }

  /** 所有速度事件登记完毕后调用, 计算累计时间 */
  public void build() {
    microsAtTempoChange.clear();
    long micros = 0;
    Long lastTick = null;
    int lastTempo = DEFAULT_MICROS_PER_BEAT;
    for (var entry : tempoChanges.entrySet()) {
      if (lastTick != null) {
        micros += ticksToMicros(entry.getKey() - lastTick, lastTempo);
      }
      microsAtTempoChange.put(entry.getKey(), micros);
      lastTick = entry.getKey();
      lastTempo = entry.getValue();
    }
  }

  private long ticksToMicros(long ticks, int microsPerBeat) {
    return Math.round(ticks * (double) microsPerBeat / resolution);
  }

  /**
   * tick 转微秒
   *
   * @param tick tick
   * @return 微秒
   */
  public long tickToMicros(long tick) {
    if (!isPpq()) {
      // SMPTE: 每秒 tick 数 = 帧率 * 每帧 tick
      return Math.round(tick * 1_000_000.0 / (divisionType * resolution));
    }
    if (microsAtTempoChange.isEmpty()) {
      build();
    }
    var floor = tempoChanges.floorEntry(tick);
    long baseTick = floor.getKey();
    return microsAtTempoChange.get(baseTick) + ticksToMicros(tick - baseTick, floor.getValue());
  }

  /** 起始速度(微秒/拍) */
  public int getInitialMicrosPerBeat() {
    return tempoChanges.firstEntry().getValue();
  }

  /**
   * 乐曲中持续时间最长的速度(微秒/拍), 用于推荐电路时钟周期
   *
   * @param endTick 乐曲结束 tick
   * @return 微秒/拍
   */
  public int getDominantMicrosPerBeat(long endTick) {
    if (tempoChanges.size() == 1) {
      return getInitialMicrosPerBeat();
    }
    TreeMap<Integer, Long> durationByTempo = new TreeMap<>();
    List<Long> ticks = new ArrayList<>(tempoChanges.keySet());
    for (int i = 0; i < ticks.size(); i++) {
      long from = ticks.get(i);
      long to = i + 1 < ticks.size() ? ticks.get(i + 1) : Math.max(endTick, from);
      int tempo = tempoChanges.get(from);
      durationByTempo.merge(tempo, Math.max(0, to - from), Long::sum);
    }
    return durationByTempo.entrySet().stream()
        .max((a, b) -> Long.compare(a.getValue(), b.getValue()))
        .map(e -> e.getKey())
        .orElse(getInitialMicrosPerBeat());
  }

  /** 不同速度的数量 */
  public int getDistinctTempoCount() {
    return (int) tempoChanges.values().stream().distinct().count();
  }

  public static double microsPerBeatToBpm(int microsPerBeat) {
    return 60_000_000.0 / microsPerBeat;
  }
}
