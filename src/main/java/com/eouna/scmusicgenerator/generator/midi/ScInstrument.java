package com.eouna.scmusicgenerator.generator.midi;

/**
 * 生存战争发音器(Sound Generator)支持的乐器.
 *
 * <p>数据来源: 游戏源码 {@code SoundGeneratorElectricElement}:
 *
 * <ul>
 *   <li>乐器端(Tone)取值: 1=Bell 2=Organ 3=Ping 4=String 5=Trumpet 6=Voice 7=Piano 8=PianoLong
 *       9=Drums A=Bass (F 也是 Piano)
 *   <li>频率公式: {@code freq = 130.8125Hz * 2^((pitch + 12 * octave) / 12)}, 130.8125Hz = C3 = MIDI
 *       48, 即 {@code MIDI音符 = 48 + 12 * octave + pitch}
 *   <li>八度上限: 采样为 C4/C5/C6, 八度 0~3 可用; Organ 只有 C4/C5, 八度 0~2 可用; 超出范围会被钳到最高音
 *   <li>鼓(9)忽略八度, 音高 0~9 选择鼓声
 * </ul>
 *
 * @author CCL
 */
public enum ScInstrument {
  /** 铃铛 */
  BELL(1, "铃铛", "Bell", 3, 48),
  /** 风琴, 只有两个八度采样 */
  ORGAN(2, "风琴", "Organ", 2, 48),
  /** 砰, 社区 Wiki 实测采样比名义音高高一个八度 */
  PING(3, "砰", "Ping", 3, 60),
  /** 弦乐 */
  STRING(4, "弦乐器", "String", 3, 48),
  /** 小号 */
  TRUMPET(5, "小号", "Trumpet", 3, 48),
  /** 人声, 社区 Wiki 实测采样比名义音高高一个八度 */
  VOICE(6, "语音", "Voice", 3, 60),
  /** 钢琴 */
  PIANO(7, "钢琴", "Piano", 3, 48),
  /** 钢琴延音 */
  PIANO_SUSTAIN(8, "钢琴延音", "PianoSustain", 3, 48),
  /** 鼓, 不响应八度 */
  DRUMS(9, "鼓", "Drums", 0, 48),
  /** 贝斯(2.4 版本加入) */
  BASS(10, "贝斯", "Bass", 3, 48);

  /** P 端的休止值, 发音器遇到 F 不发声 */
  public static final int PAUSE_PITCH = 15;

  /** 鼓声数量: 0=Snare 1=BassDrum 2=ClosedHiHat 3=PedalHiHat 4=OpenHiHat 5=LowTom 6=HighTom 7=Crash 8=Ride 9=HandClap */
  public static final int DRUM_SOUND_COUNT = 10;

  /** 鼓声名称, 下标即 P 端取值 */
  public static final String[] DRUM_SOUND_NAMES = {
    "Snare(军鼓)",
    "BassDrum(底鼓)",
    "ClosedHiHat(闭镲)",
    "PedalHiHat(踩镲)",
    "OpenHiHat(开镲)",
    "LowTom(低音桶鼓)",
    "HighTom(高音桶鼓)",
    "Crash(吊镲)",
    "Ride(叮叮镲)",
    "HandClap(拍手)"
  };

  private static final char[] HEX = "0123456789ABCDEF".toCharArray();

  /** 乐器端取值 */
  private final int code;

  /** 中文名 */
  private final String cnName;

  /** 英文名 */
  private final String enName;

  /** 可用的最高八度(含) */
  private final int maxOctave;

  /** 八度 0 音高 0 实际发出的 MIDI 音符 */
  private final int baseMidiNote;

  ScInstrument(int code, String cnName, String enName, int maxOctave, int baseMidiNote) {
    this.code = code;
    this.cnName = cnName;
    this.enName = enName;
    this.maxOctave = maxOctave;
    this.baseMidiNote = baseMidiNote;
  }

  public int getCode() {
    return code;
  }

  /** 乐器端的十六进制字符 */
  public char getCodeChar() {
    return HEX[code];
  }

  public String getCnName() {
    return cnName;
  }

  public String getEnName() {
    return enName;
  }

  public int getMaxOctave() {
    return maxOctave;
  }

  public boolean isDrums() {
    return this == DRUMS;
  }

  /** 该乐器能发出的最低 MIDI 音符(八度 0, 音高 0) */
  public int getLowestMidiNote() {
    return baseMidiNote;
  }

  /**
   * 该乐器能发出的最高 MIDI 音符(含). 最高八度配合音高 C(12) 可以多发一个 C, 例如钢琴 (3, C) = C7
   *
   * @return 最高 MIDI 音符
   */
  public int getHighestMidiNote() {
    return baseMidiNote + 12 * (maxOctave + 1);
  }

  /** 该乐器的音域跨度(半音数) */
  public int getRangeInSemitones() {
    return getHighestMidiNote() - getLowestMidiNote() + 1;
  }

  public static ScInstrument ofCode(int code) {
    for (ScInstrument instrument : values()) {
      if (instrument.code == code) {
        return instrument;
      }
    }
    return null;
  }

  public static char hexChar(int value) {
    return HEX[Math.max(0, Math.min(15, value))];
  }

  @Override
  public String toString() {
    return cnName + "(" + enName + ")";
  }
}
