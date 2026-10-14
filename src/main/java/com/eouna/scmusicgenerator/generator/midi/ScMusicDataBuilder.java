package com.eouna.scmusicgenerator.generator.midi;

import com.eouna.scmusicgenerator.constant.DefaultEnvConfigConstant;
import com.eouna.scmusicgenerator.core.window.IWindowLogger;
import com.eouna.scmusicgenerator.generator.midi.ScMusicData.ScPage;
import com.eouna.scmusicgenerator.generator.midi.ScMusicData.ScPlate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 把解码后的 MIDI 按键转换成生存战争存储板数据.
 *
 * <p>核心思路: 所有乐器共用同一条时间网格, 网格的一格 = 一个量化步长 = 电路时钟的一个周期(脉冲模式下为两个周期). 第 i 格永远对应乐曲的第 i
 * 个步长, 没有音符的格子写休止(P=F, V=0), 这样不同乐器之间、长短音之间的时间关系才能还原.
 *
 * @author CCL
 */
public class ScMusicDataBuilder {

  /** 发音器两次触发的最小间隔(秒), 来自游戏源码 */
  public static final double MIN_RETRIGGER_SECONDS = 0.08;

  /** 游戏能同时播放的乐器数有限, 超过这个数量提示用户 */
  public static final int SIMULTANEOUS_SOUND_HINT = 6;

  /** 构建选项 */
  public static final class Options {
    private QuantizeStep step = QuantizeStep.defaultStep();
    private ToneMode toneMode = ToneMode.CONSTANT;
    private boolean normalizeVelocity = true;

    /** 每种乐器最多使用的发音器数量, 0 表示不限制; 超出的音(从低音开始)会被丢弃 */
    private int maxPlatesPerInstrument = 0;

    public QuantizeStep getStep() {
      return step;
    }

    public Options setStep(QuantizeStep step) {
      this.step = Objects.requireNonNull(step);
      return this;
    }

    public ToneMode getToneMode() {
      return toneMode;
    }

    public Options setToneMode(ToneMode toneMode) {
      this.toneMode = Objects.requireNonNull(toneMode);
      return this;
    }

    public boolean isNormalizeVelocity() {
      return normalizeVelocity;
    }

    public Options setNormalizeVelocity(boolean normalizeVelocity) {
      this.normalizeVelocity = normalizeVelocity;
      return this;
    }

    public int getMaxPlatesPerInstrument() {
      return maxPlatesPerInstrument;
    }

    public Options setMaxPlatesPerInstrument(int maxPlatesPerInstrument) {
      this.maxPlatesPerInstrument = Math.max(0, maxPlatesPerInstrument);
      return this;
    }
  }

  /** 一个格子里的一个音 */
  private static final class Cell {
    final int pitch;
    final int octave;
    final int volume;

    /** 排序用: 旋律乐器为移调后的 MIDI 音符, 鼓为鼓声编号 */
    final int sortKey;

    final long sourceTick;

    Cell(int pitch, int octave, int volume, int sortKey, long sourceTick) {
      this.pitch = pitch;
      this.octave = octave;
      this.volume = volume;
      this.sortKey = sortKey;
      this.sourceTick = sourceTick;
    }
  }

  /** 单个乐器的统计 */
  private static final class InstrumentStat {
    int transposeOctaves;
    int foldedNotes;
    int mergedSteps;
    int duplicateNotes;
    int droppedByPolyphony;
    int unmappedDrumNotes;
    final Set<Integer> unmappedDrumNoteNumbers = new TreeSet<>();
    int polyphony;
    int noteCount;
  }

  /**
   * 构建音乐数据
   *
   * @param logger 日志
   * @param decoder 已完成解码的解码器
   * @param options 选项
   * @return 音乐数据, 没有可用音符时返回空数据
   */
  public ScMusicData build(IWindowLogger logger, MidiFileDecoder decoder, Options options) {
    Objects.requireNonNull(options);
    QuantizeStep step = options.getStep();
    ToneMode toneMode = options.getToneMode();
    MidiTempoMap tempoMap = decoder.getTempoMap();
    Map<ScInstrument, List<MidiNote>> notesByInstrument = decoder.getNotesByInstrument();

    // ---------- 1. 时间网格 ----------
    int microsPerBeat;
    if (tempoMap.isPpq()) {
      microsPerBeat = tempoMap.getDominantMicrosPerBeat(decoder.getLastNoteTick());
    } else {
      microsPerBeat = MidiTempoMap.DEFAULT_MICROS_PER_BEAT;
      if (step.isBeatBased()) {
        logger.warn("SMPTE 计时的 MIDI 没有拍的概念, 按拍量化时按 120 BPM 处理, 建议改用固定毫秒步长");
      }
    }
    double stepMicros = step.stepMicros(microsPerBeat);
    double bpm = MidiTempoMap.microsPerBeatToBpm(microsPerBeat);
    int cellsPerStep = toneMode.getCellsPerStep();
    int stepsPerPage = DefaultEnvConfigConstant.SC_MUSIC_BLOCK_MAX_STORAGE_CHAR / cellsPerStep;
    double cellPeriodSeconds = stepMicros / cellsPerStep / 1_000_000.0;

    // ---------- 2. 响度换算 ----------
    int velocityCeiling = 127;
    if (options.isNormalizeVelocity()) {
      velocityCeiling =
          Math.max(1, decoder.getNotes().stream().mapToInt(n -> n.velocity).max().orElse(127));
    }

    // ---------- 3. 量化 + 音高映射 ----------
    // 去掉开头的静音: 第一个按键所在的格子作为第 0 格
    int firstStep =
        decoder.getNotes().stream()
            .mapToInt(n -> quantize(n.tick, step, tempoMap, stepMicros))
            .min()
            .orElse(0);
    if (firstStep > 0) {
      logger.info("已去掉乐曲开头 {} 步的静音", firstStep);
    }
    LinkedHashMap<ScInstrument, TreeMap<Integer, List<Cell>>> grid = new LinkedHashMap<>();
    Map<ScInstrument, InstrumentStat> stats = new LinkedHashMap<>();
    int maxStepIndex = -1;
    for (Map.Entry<ScInstrument, List<MidiNote>> entry : notesByInstrument.entrySet()) {
      ScInstrument instrument = entry.getKey();
      List<MidiNote> notes = entry.getValue();
      InstrumentStat stat = new InstrumentStat();
      stats.put(instrument, stat);
      stat.transposeOctaves = instrument.isDrums() ? 0 : chooseTransposition(instrument, notes);

      TreeMap<Integer, List<Cell>> steps = new TreeMap<>();
      for (MidiNote note : notes) {
        int stepIndex = quantize(note.tick, step, tempoMap, stepMicros) - firstStep;
        Cell cell = toCell(instrument, note, stat, velocityCeiling);
        if (cell == null) {
          continue;
        }
        steps.computeIfAbsent(stepIndex, k -> new ArrayList<>()).add(cell);
        maxStepIndex = Math.max(maxStepIndex, stepIndex);
      }
      // 同格整理: 去重, 排序, 限制复音数
      for (Map.Entry<Integer, List<Cell>> stepEntry : steps.entrySet()) {
        List<Cell> cells = stepEntry.getValue();
        long distinctTicks = cells.stream().mapToLong(c -> c.sourceTick).distinct().count();
        if (distinctTicks > 1) {
          stat.mergedSteps++;
        }
        List<Cell> cleaned = dedupeAndSort(cells, instrument);
        stat.duplicateNotes += cells.size() - cleaned.size();
        int limit = options.getMaxPlatesPerInstrument();
        if (limit > 0 && cleaned.size() > limit) {
          stat.droppedByPolyphony += cleaned.size() - limit;
          cleaned = new ArrayList<>(cleaned.subList(0, limit));
        }
        stepEntry.setValue(cleaned);
        stat.polyphony = Math.max(stat.polyphony, cleaned.size());
        stat.noteCount += cleaned.size();
      }
      steps.values().removeIf(List::isEmpty);
      if (!steps.isEmpty()) {
        grid.put(instrument, steps);
      }
    }

    if (grid.isEmpty() || maxStepIndex < 0) {
      logger.warn("没有可以转换的音符");
      return new ScMusicData(
          step, toneMode, 0, stepsPerPage, 0, cellPeriodSeconds, bpm, new LinkedHashMap<>(), Map.of());
    }

    // ---------- 4. 生成存储板数据 ----------
    int totalSteps = maxStepIndex + 1;
    int pageCount = (totalSteps + stepsPerPage - 1) / stepsPerPage;
    LinkedHashMap<ScInstrument, List<ScPlate>> plates = new LinkedHashMap<>();
    Map<ScInstrument, Integer> transposeOctaves = new LinkedHashMap<>();
    for (Map.Entry<ScInstrument, TreeMap<Integer, List<Cell>>> entry : grid.entrySet()) {
      ScInstrument instrument = entry.getKey();
      InstrumentStat stat = stats.get(instrument);
      transposeOctaves.put(instrument, stat.transposeOctaves);
      List<ScPlate> instrumentPlates = new ArrayList<>();
      for (int voice = 0; voice < stat.polyphony; voice++) {
        instrumentPlates.add(
            buildPlate(
                instrument, voice, entry.getValue(), pageCount, stepsPerPage, toneMode));
      }
      plates.put(instrument, instrumentPlates);
    }
    ScMusicData data =
        new ScMusicData(
            step,
            toneMode,
            totalSteps,
            stepsPerPage,
            pageCount,
            cellPeriodSeconds,
            bpm,
            plates,
            transposeOctaves);
    logSummary(logger, data, stats, stepMicros);
    return data;
  }

  /**
   * 计算按键所在的格子, 四舍五入到最近的格
   *
   * @return 格子序号(从 0 开始)
   */
  private int quantize(long tick, QuantizeStep step, MidiTempoMap tempoMap, double stepMicros) {
    if (step.isBeatBased() && tempoMap.isPpq()) {
      long resolution = tempoMap.getResolution();
      // round(tick * stepsPerBeat / resolution) 的整数写法
      long numerator = tick * step.getStepsPerBeat() * 2 + resolution;
      return (int) (numerator / (2 * resolution));
    }
    long micros = tempoMap.tickToMicros(tick);
    return (int) Math.floor(micros / stepMicros + 0.5);
  }

  /** 旋律音(同一时刻的最高音)在选移调量时的权重 */
  static final int MELODY_WEIGHT = 2;

  /** 每移调一个八度的惩罚(占总权重的比例), 避免为了很少的收益把整首歌升/降八度 */
  static final double TRANSPOSE_PENALTY_PER_OCTAVE = 0.03;

  /**
   * 为乐器选择整八度移调量.
   *
   * <p>目标: 让落在乐器音域内的音符(加权)最多. 同一时刻的最高音通常是旋律, 权重更高, 这样宁可把低音折叠上来也尽量保住旋律; 每移调一个八度扣除
   * 3% 的分数, 使得收益不明显时保持原始音高.
   *
   * @return 八度数, 正数表示升高
   */
  static int chooseTransposition(ScInstrument instrument, List<MidiNote> notes) {
    if (notes.isEmpty()) {
      return 0;
    }
    // 每个 tick 的最高音
    Map<Long, Integer> topNoteByTick = new HashMap<>();
    for (MidiNote note : notes) {
      topNoteByTick.merge(note.tick, note.note, Math::max);
    }
    int[] weights = new int[notes.size()];
    double totalWeight = 0;
    for (int i = 0; i < notes.size(); i++) {
      MidiNote note = notes.get(i);
      weights[i] = note.note == topNoteByTick.get(note.tick) ? MELODY_WEIGHT : 1;
      totalWeight += weights[i];
    }
    int bestK = 0;
    double bestScore = -Double.MAX_VALUE;
    for (int k = -4; k <= 4; k++) {
      int shift = 12 * k;
      double inRange = 0;
      for (int i = 0; i < notes.size(); i++) {
        int n = notes.get(i).note + shift;
        if (n >= instrument.getLowestMidiNote() && n <= instrument.getHighestMidiNote()) {
          inRange += weights[i];
        }
      }
      double score = inRange / totalWeight - TRANSPOSE_PENALTY_PER_OCTAVE * Math.abs(k);
      boolean better =
          score > bestScore + 1e-9
              || (Math.abs(score - bestScore) <= 1e-9
                  && (Math.abs(k) < Math.abs(bestK)
                      || (Math.abs(k) == Math.abs(bestK) && k < bestK)));
      if (better) {
        bestScore = score;
        bestK = k;
      }
    }
    return bestK;
  }

  /**
   * 把一个 MIDI 按键换算成格子数据
   *
   * @return 无法表达时返回 null
   */
  private Cell toCell(ScInstrument instrument, MidiNote note, InstrumentStat stat, int velocityCeiling) {
    int volume = (int) Math.round(note.velocity * 15.0 / velocityCeiling);
    volume = Math.max(1, Math.min(15, volume));
    if (instrument.isDrums()) {
      Integer drumPitch = GmInstrumentMapping.drumPitchOf(note.channel, note.program, note.note);
      if (drumPitch == null) {
        stat.unmappedDrumNotes++;
        stat.unmappedDrumNoteNumbers.add(note.note);
        return null;
      }
      return new Cell(drumPitch, 0, volume, drumPitch, note.tick);
    }
    int lowest = instrument.getLowestMidiNote();
    int highest = instrument.getHighestMidiNote();
    int n = note.note + 12 * stat.transposeOctaves;
    if (n < lowest) {
      n += 12 * ((lowest - n + 11) / 12);
      stat.foldedNotes++;
    } else if (n > highest) {
      n -= 12 * ((n - highest + 11) / 12);
      stat.foldedNotes++;
    }
    int rel = n - lowest;
    int octave = rel / 12;
    int pitch = rel % 12;
    if (octave > instrument.getMaxOctave()) {
      // 只有 n == highest 时会到这里: 用最高八度的 C(12) 表示
      octave = instrument.getMaxOctave();
      pitch = 12;
    }
    return new Cell(pitch, octave, volume, n, note.tick);
  }

  /** 同一格内去掉重复音(保留响度最大的), 旋律乐器按音高从高到低排, 鼓按鼓声编号排 */
  private List<Cell> dedupeAndSort(List<Cell> cells, ScInstrument instrument) {
    Map<Integer, Cell> byPitch = new LinkedHashMap<>();
    for (Cell cell : cells) {
      int key = cell.octave * 16 + cell.pitch;
      Cell existing = byPitch.get(key);
      if (existing == null || cell.volume > existing.volume) {
        byPitch.put(key, cell);
      }
    }
    List<Cell> result = new ArrayList<>(byPitch.values());
    if (instrument.isDrums()) {
      result.sort(Comparator.comparingInt((Cell c) -> c.sortKey));
    } else {
      result.sort(Comparator.comparingInt((Cell c) -> c.sortKey).reversed());
    }
    return result;
  }

  private ScPlate buildPlate(
      ScInstrument instrument,
      int voice,
      TreeMap<Integer, List<Cell>> steps,
      int pageCount,
      int stepsPerPage,
      ToneMode toneMode) {
    char toneChar = instrument.getCodeChar();
    char pauseChar = ScInstrument.hexChar(ScInstrument.PAUSE_PITCH);
    List<ScPage> pages = new ArrayList<>(pageCount);
    for (int page = 0; page < pageCount; page++) {
      StringBuilder pitch = new StringBuilder(DefaultEnvConfigConstant.SC_MUSIC_BLOCK_MAX_STORAGE_CHAR);
      StringBuilder octave = new StringBuilder(DefaultEnvConfigConstant.SC_MUSIC_BLOCK_MAX_STORAGE_CHAR);
      StringBuilder volume = new StringBuilder(DefaultEnvConfigConstant.SC_MUSIC_BLOCK_MAX_STORAGE_CHAR);
      StringBuilder tone = new StringBuilder(DefaultEnvConfigConstant.SC_MUSIC_BLOCK_MAX_STORAGE_CHAR);
      int noteCount = 0;
      for (int s = 0; s < stepsPerPage; s++) {
        int stepIndex = page * stepsPerPage + s;
        List<Cell> cells = steps.get(stepIndex);
        Cell cell = cells != null && cells.size() > voice ? cells.get(voice) : null;
        if (cell != null) {
          noteCount++;
          pitch.append(ScInstrument.hexChar(cell.pitch));
          octave.append(ScInstrument.hexChar(cell.octave));
          volume.append(ScInstrument.hexChar(cell.volume));
          tone.append(toneChar);
        } else {
          pitch.append(pauseChar);
          octave.append('0');
          volume.append('0');
          tone.append(toneMode == ToneMode.CONSTANT ? toneChar : '0');
        }
        if (toneMode == ToneMode.PULSE) {
          // 补一格 0, 让乐器端回到 0, 下一步才能产生上升沿
          pitch.append(pauseChar);
          octave.append('0');
          volume.append('0');
          tone.append('0');
        }
      }
      pages.add(
          new ScPage(
              page, pitch.toString(), octave.toString(), volume.toString(), tone.toString(), noteCount));
    }
    return new ScPlate(instrument, voice, pages);
  }

  private void logSummary(
      IWindowLogger logger,
      ScMusicData data,
      Map<ScInstrument, InstrumentStat> stats,
      double stepMicros) {
    QuantizeStep step = data.getStep();
    String stepDesc =
        step.isBeatBased()
            ? String.format("%s, 按 %.1f BPM 计算", step.getLabel(), data.getBpm())
            : step.getLabel();
    logger.info(
        "量化步长: {} => 每步 {} 毫秒; 乐器端模式: {}",
        stepDesc,
        String.format("%.1f", stepMicros / 1000.0),
        data.getToneMode().getLabel());
    logger.success(
        "电路时钟周期(每个存储格的时长): {} 秒, 即每分钟 {} 个脉冲",
        String.format("%.4f", data.getCellPeriodSeconds()),
        String.format("%.1f", 60.0 / data.getCellPeriodSeconds()));
    logger.info(
        "乐曲共 {} 步, 每页 {} 步, 共 {} 页; 需要发音器 {} 块, 存储板 {} 块",
        data.getTotalSteps(),
        data.getStepsPerPage(),
        data.getPageCount(),
        data.getPlateCount(),
        data.getMemoryBankCount());
    if (stepMicros / 1_000_000.0 < MIN_RETRIGGER_SECONDS) {
      logger.warn(
          "每步时长 {} 毫秒小于发音器的最小触发间隔 80 毫秒, 连续的音符可能发不出声, 建议换更大的步长",
          String.format("%.1f", stepMicros / 1000.0));
    }
    int mergedTotal = 0;
    for (Map.Entry<ScInstrument, InstrumentStat> entry : stats.entrySet()) {
      ScInstrument instrument = entry.getKey();
      InstrumentStat stat = entry.getValue();
      if (stat.noteCount == 0) {
        continue;
      }
      StringBuilder sb = new StringBuilder();
      sb.append("乐器 [").append(instrument).append("] 声部 ").append(stat.polyphony);
      sb.append(", 音符 ").append(stat.noteCount);
      if (!instrument.isDrums()) {
        sb.append(", 音域 ")
            .append(InstrumentInfo.midiNoteName(instrument.getLowestMidiNote()))
            .append("~")
            .append(InstrumentInfo.midiNoteName(instrument.getHighestMidiNote()));
        if (stat.transposeOctaves != 0) {
          sb.append(", 整体移调 ")
              .append(stat.transposeOctaves > 0 ? "+" : "")
              .append(stat.transposeOctaves)
              .append(" 个八度");
        }
        if (stat.foldedNotes > 0) {
          sb.append(", 超出音域折叠 ").append(stat.foldedNotes).append(" 个");
        }
      }
      if (stat.duplicateNotes > 0) {
        sb.append(", 同格重复音去重 ").append(stat.duplicateNotes).append(" 个");
      }
      if (stat.mergedSteps > 0) {
        sb.append(", 合并了不同时刻的格子 ").append(stat.mergedSteps).append(" 个");
        mergedTotal += stat.mergedSteps;
      }
      if (stat.droppedByPolyphony > 0) {
        sb.append(", 因声部上限丢弃 ").append(stat.droppedByPolyphony).append(" 个");
      }
      logger.info(sb.toString());
      if (stat.unmappedDrumNotes > 0) {
        logger.warn(
            "鼓组中有 {} 个音没有对应的游戏鼓声, 已丢弃: {}",
            stat.unmappedDrumNotes,
            stat.unmappedDrumNoteNumbers.stream()
                .map(n -> n + " " + GmInstrumentMapping.percussionNoteName(n))
                .collect(Collectors.joining(", ")));
      }
      if (stat.polyphony > SIMULTANEOUS_SOUND_HINT) {
        logger.warn(
            "乐器 [{}] 需要 {} 个声部, 游戏同时播放的声音有限, 和弦过密时可考虑更粗的步长或限制声部数",
            instrument,
            stat.polyphony);
      }
    }
    if (mergedTotal > 0) {
      logger.warn(
          "共有 {} 个格子合并了不同时刻的音符(被当作和弦, 会增加声部数), 如需保留节奏细节请选择更小的步长",
          mergedTotal);
    }
  }
}
