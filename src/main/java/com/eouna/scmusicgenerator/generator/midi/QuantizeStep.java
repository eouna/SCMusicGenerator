package com.eouna.scmusicgenerator.generator.midi;

import java.util.List;

/**
 * 时间量化步长: 存储板的一个格子代表的时间长度.
 *
 * <p>两种定义方式:
 *
 * <ul>
 *   <li>按拍: 一拍(四分音符)分成 N 格, 格子时长随 MIDI 速度变化; 这是乐谱意义上的量化, 推荐使用
 *   <li>固定毫秒: 每格固定时长, 与 MIDI 速度无关, 适合没有速度信息或变速很多的文件
 * </ul>
 *
 * @author CCL
 */
public final class QuantizeStep {

  /** 预设列表(用于下拉框) */
  private static final List<QuantizeStep> PRESETS =
      List.of(
          ofBeatFraction(4, "1/4 拍 (十六分音符)"),
          ofBeatFraction(2, "1/2 拍 (八分音符)"),
          ofBeatFraction(1, "1 拍 (四分音符)"),
          ofBeatFraction(3, "1/3 拍 (八分三连音)"),
          ofBeatFraction(6, "1/6 拍 (十六分三连音)"),
          ofBeatFraction(8, "1/8 拍 (三十二分音符)"),
          ofMillis(100, "固定 100 毫秒"),
          ofMillis(125, "固定 125 毫秒"),
          ofMillis(150, "固定 150 毫秒"),
          ofMillis(200, "固定 200 毫秒"),
          ofMillis(250, "固定 250 毫秒"));

  /** 每拍格数, 按拍量化时 > 0 */
  private final int stepsPerBeat;

  /** 每格毫秒数, 固定时长量化时 > 0 */
  private final int fixedMillis;

  private final String label;

  private QuantizeStep(int stepsPerBeat, int fixedMillis, String label) {
    this.stepsPerBeat = stepsPerBeat;
    this.fixedMillis = fixedMillis;
    this.label = label;
  }

  public static QuantizeStep ofBeatFraction(int stepsPerBeat, String label) {
    if (stepsPerBeat <= 0) {
      throw new IllegalArgumentException("stepsPerBeat 必须大于 0");
    }
    return new QuantizeStep(stepsPerBeat, 0, label);
  }

  public static QuantizeStep ofBeatFraction(int stepsPerBeat) {
    return ofBeatFraction(stepsPerBeat, "1/" + stepsPerBeat + " 拍");
  }

  public static QuantizeStep ofMillis(int millis, String label) {
    if (millis <= 0) {
      throw new IllegalArgumentException("millis 必须大于 0");
    }
    return new QuantizeStep(0, millis, label);
  }

  public static QuantizeStep ofMillis(int millis) {
    return ofMillis(millis, "固定 " + millis + " 毫秒");
  }

  public static List<QuantizeStep> presets() {
    return PRESETS;
  }

  /** 默认步长: 1/4 拍 */
  public static QuantizeStep defaultStep() {
    return PRESETS.get(0);
  }

  public boolean isBeatBased() {
    return stepsPerBeat > 0;
  }

  public int getStepsPerBeat() {
    return stepsPerBeat;
  }

  public int getFixedMillis() {
    return fixedMillis;
  }

  public String getLabel() {
    return label;
  }

  /**
   * 计算每格的微秒数
   *
   * @param microsPerBeat 每拍微秒数(按拍量化时使用)
   * @return 微秒
   */
  public double stepMicros(int microsPerBeat) {
    return isBeatBased() ? microsPerBeat / (double) stepsPerBeat : fixedMillis * 1000.0;
  }

  @Override
  public String toString() {
    return label;
  }
}
