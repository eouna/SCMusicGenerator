package com.eouna.scmusicgenerator.generator.midi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成好的生存战争音乐数据.
 *
 * <p>结构: 乐器 -> 若干块发音器(声部) -> 若干页(每页对应 4 块存储板, 各 256 个字符).
 *
 * @author CCL
 */
public final class ScMusicData {

  /** 一页 = 一组(P/O/V/乐器端)存储板的数据 */
  public static final class ScPage {
    /** 页码, 从 0 开始 */
    public final int pageIndex;

    public final String pitchData;
    public final String octaveData;
    public final String volumeData;
    public final String toneData;

    /** 本页的音符数 */
    public final int noteCount;

    public ScPage(
        int pageIndex,
        String pitchData,
        String octaveData,
        String volumeData,
        String toneData,
        int noteCount) {
      this.pageIndex = pageIndex;
      this.pitchData = pitchData;
      this.octaveData = octaveData;
      this.volumeData = volumeData;
      this.toneData = toneData;
      this.noteCount = noteCount;
    }
  }

  /** 一块发音器(一个声部) 的全部数据 */
  public static final class ScPlate {
    public final ScInstrument instrument;

    /** 同一乐器内的序号, 从 0 开始 */
    public final int index;

    public final List<ScPage> pages;

    /** 本声部的音符数 */
    public final int noteCount;

    public ScPlate(ScInstrument instrument, int index, List<ScPage> pages) {
      this.instrument = instrument;
      this.index = index;
      this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
      this.noteCount = pages.stream().mapToInt(p -> p.noteCount).sum();
    }

    /** 展示名, 例如 "钢琴1" */
    public String getDisplayName() {
      return instrument.getCnName() + (index + 1);
    }

    public String getDisplayEnName() {
      return instrument.getEnName() + (index + 1);
    }
  }

  private final QuantizeStep step;
  private final ToneMode toneMode;

  /** 乐曲占用的量化步数 */
  private final int totalSteps;

  /** 每页可容纳的步数 */
  private final int stepsPerPage;

  /** 页数(每个声部都是这么多页) */
  private final int pageCount;

  /** 每个格子(存储板单元) 的时长, 即电路时钟周期(秒) */
  private final double cellPeriodSeconds;

  /** 计算周期用的 BPM */
  private final double bpm;

  /** 乐器 -> 声部列表 */
  private final LinkedHashMap<ScInstrument, List<ScPlate>> plates;

  /** 各乐器的整八度移调量(单位: 八度, 正数为升) */
  private final Map<ScInstrument, Integer> transposeOctaves;

  public ScMusicData(
      QuantizeStep step,
      ToneMode toneMode,
      int totalSteps,
      int stepsPerPage,
      int pageCount,
      double cellPeriodSeconds,
      double bpm,
      LinkedHashMap<ScInstrument, List<ScPlate>> plates,
      Map<ScInstrument, Integer> transposeOctaves) {
    this.step = step;
    this.toneMode = toneMode;
    this.totalSteps = totalSteps;
    this.stepsPerPage = stepsPerPage;
    this.pageCount = pageCount;
    this.cellPeriodSeconds = cellPeriodSeconds;
    this.bpm = bpm;
    this.plates = plates;
    this.transposeOctaves = transposeOctaves;
  }

  public QuantizeStep getStep() {
    return step;
  }

  public ToneMode getToneMode() {
    return toneMode;
  }

  public int getTotalSteps() {
    return totalSteps;
  }

  public int getStepsPerPage() {
    return stepsPerPage;
  }

  public int getPageCount() {
    return pageCount;
  }

  public double getCellPeriodSeconds() {
    return cellPeriodSeconds;
  }

  public double getBpm() {
    return bpm;
  }

  public Map<ScInstrument, List<ScPlate>> getPlates() {
    return plates;
  }

  public Map<ScInstrument, Integer> getTransposeOctaves() {
    return transposeOctaves;
  }

  /** 所有声部(按乐器顺序展开) */
  public List<ScPlate> getAllPlates() {
    List<ScPlate> all = new ArrayList<>();
    plates.values().forEach(all::addAll);
    return all;
  }

  /** 需要的发音器数量 */
  public int getPlateCount() {
    return plates.values().stream().mapToInt(List::size).sum();
  }

  /** 需要的存储板数量(每个声部每页 4 块) */
  public int getMemoryBankCount() {
    return getPlateCount() * pageCount * 4;
  }

  public boolean isEmpty() {
    return plates.isEmpty();
  }
}
