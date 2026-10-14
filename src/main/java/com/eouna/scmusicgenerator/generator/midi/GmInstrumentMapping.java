package com.eouna.scmusicgenerator.generator.midi;

import java.util.HashMap;
import java.util.Map;

/**
 * General MIDI 音色 / 打击乐音符 到生存战争乐器的映射.
 *
 * <p>MIDI Program Change 的取值是 0~127(0 = Acoustic Grand Piano), 本表全部使用 0 基编号; 通道 10(索引 9)按 GM
 * 规范固定为打击乐, 与 Program 无关.
 *
 * @author CCL
 */
public final class GmInstrumentMapping {

  /** GM 打击乐通道索引(第 10 通道) */
  public static final int PERCUSSION_CHANNEL = 9;

  /** GM Program(0 基) -> SC 乐器, null 表示不支持(音效类) */
  private static final ScInstrument[] PROGRAM_TO_INSTRUMENT = new ScInstrument[128];

  /** GM 打击乐音符 -> SC 鼓声(0~9) */
  private static final Map<Integer, Integer> PERCUSSION_NOTE_TO_DRUM = new HashMap<>();

  /** 非鼓通道上的打击类音色(太鼓/合成鼓等) 固定对应的鼓声, 这些音色的音符号没有音高意义 */
  private static final Map<Integer, Integer> PERCUSSIVE_PROGRAM_TO_DRUM = new HashMap<>();

  private GmInstrumentMapping() {}

  static {
    // 0-7 Piano
    fill(0, 7, ScInstrument.PIANO);
    // 8-15 Chromatic Percussion
    fill(8, 11, ScInstrument.BELL); // Celesta Glockenspiel MusicBox Vibraphone
    fill(12, 13, ScInstrument.PING); // Marimba Xylophone
    put(14, ScInstrument.BELL); // Tubular Bells
    put(15, ScInstrument.PING); // Dulcimer
    // 16-23 Organ
    fill(16, 23, ScInstrument.ORGAN);
    // 24-31 Guitar
    fill(24, 27, ScInstrument.PIANO); // Nylon Steel Jazz Clean
    put(28, ScInstrument.PING); // Muted
    fill(29, 30, ScInstrument.ORGAN); // Overdriven Distortion
    put(31, ScInstrument.BELL); // Harmonics
    // 32-39 Bass
    fill(32, 39, ScInstrument.BASS);
    // 40-47 Strings
    fill(40, 44, ScInstrument.STRING); // Violin Viola Cello Contrabass Tremolo
    put(45, ScInstrument.PIANO); // Pizzicato
    put(46, ScInstrument.STRING); // Harp
    put(47, ScInstrument.BASS); // Timpani
    // 48-55 Ensemble
    fill(48, 51, ScInstrument.STRING); // String Ensemble 1/2, Synth Strings 1/2
    fill(52, 54, ScInstrument.VOICE); // Choir Aahs, Voice Oohs, Synth Voice
    put(55, ScInstrument.TRUMPET); // Orchestra Hit
    // 56-63 Brass
    fill(56, 63, ScInstrument.TRUMPET);
    // 64-71 Reed
    fill(64, 67, ScInstrument.TRUMPET); // Saxes
    fill(68, 70, ScInstrument.STRING); // Oboe EnglishHorn Bassoon
    put(71, ScInstrument.ORGAN); // Clarinet
    // 72-79 Pipe
    fill(72, 79, ScInstrument.ORGAN);
    // 80-87 Synth Lead
    put(80, ScInstrument.ORGAN); // square
    put(81, ScInstrument.TRUMPET); // sawtooth
    put(82, ScInstrument.ORGAN); // calliope
    put(83, ScInstrument.ORGAN); // chiff
    put(84, ScInstrument.TRUMPET); // charang
    put(85, ScInstrument.VOICE); // voice
    put(86, ScInstrument.ORGAN); // fifths
    put(87, ScInstrument.BASS); // bass + lead
    // 88-95 Synth Pad
    put(88, ScInstrument.BELL); // new age
    put(89, ScInstrument.STRING); // warm
    put(90, ScInstrument.ORGAN); // polysynth
    put(91, ScInstrument.VOICE); // choir
    put(92, ScInstrument.STRING); // bowed
    put(93, ScInstrument.BELL); // metallic
    put(94, ScInstrument.VOICE); // halo
    put(95, ScInstrument.STRING); // sweep
    // 96-103 Synth Effects
    fill(96, 98, ScInstrument.PING); // rain soundtrack crystal
    put(99, ScInstrument.STRING); // atmosphere
    put(100, ScInstrument.BELL); // brightness
    put(101, ScInstrument.VOICE); // goblins
    fill(102, 103, ScInstrument.PING); // echoes sci-fi
    // 104-111 Ethnic
    fill(104, 107, ScInstrument.PIANO); // Sitar Banjo Shamisen Koto
    put(108, ScInstrument.BELL); // Kalimba
    put(109, ScInstrument.ORGAN); // Bagpipe
    put(110, ScInstrument.STRING); // Fiddle
    put(111, ScInstrument.TRUMPET); // Shanai
    // 112-119 Percussive
    fill(112, 113, ScInstrument.BELL); // Tinkle Bell, Agogo
    fill(114, 115, ScInstrument.PING); // Steel Drums, Woodblock
    fill(116, 119, ScInstrument.DRUMS); // Taiko, Melodic Tom, Synth Drum, Reverse Cymbal
    PERCUSSIVE_PROGRAM_TO_DRUM.put(116, 1);
    PERCUSSIVE_PROGRAM_TO_DRUM.put(117, 5);
    PERCUSSIVE_PROGRAM_TO_DRUM.put(118, 0);
    PERCUSSIVE_PROGRAM_TO_DRUM.put(119, 7);
    // 120-127 Sound Effects: 无法用乐器表达, 丢弃

    // GM 打击乐音符 -> SC 鼓: 0=Snare 1=BassDrum 2=ClosedHiHat 3=PedalHiHat 4=OpenHiHat
    // 5=LowTom 6=HighTom 7=Crash 8=Ride 9=HandClap
    drum(1, 35, 36); // Acoustic Bass Drum, Bass Drum 1
    drum(0, 37, 38, 40); // Side Stick, Acoustic Snare, Electric Snare
    drum(9, 39); // Hand Clap
    drum(5, 41, 43, 45); // Low Floor Tom, High Floor Tom, Low Tom
    drum(6, 47, 48, 50); // Low-Mid Tom, Hi-Mid Tom, High Tom
    drum(2, 42); // Closed Hi-Hat
    drum(3, 44); // Pedal Hi-Hat
    drum(4, 46); // Open Hi-Hat
    drum(7, 49, 52, 55, 57); // Crash 1, Chinese Cymbal, Splash, Crash 2
    drum(8, 51, 53, 59); // Ride 1, Ride Bell, Ride 2
    drum(2, 54); // Tambourine
    drum(8, 56); // Cowbell
    drum(6, 60, 62, 63, 65); // High Bongo, Mute/Open High Conga, High Timbale
    drum(5, 61, 64, 66); // Low Bongo, Low Conga, Low Timbale
    drum(8, 67, 68, 80, 81); // High/Low Agogo, Mute/Open Triangle
    drum(2, 69, 70, 73); // Cabasa, Maracas, Short Guiro
    drum(4, 74); // Long Guiro
    drum(9, 75, 76, 77); // Claves, High/Low Wood Block
    // 58 Vibraslap, 71/72 Whistle, 78/79 Cuica 没有合适的鼓声, 丢弃
  }

  private static void fill(int from, int to, ScInstrument instrument) {
    for (int program = from; program <= to; program++) {
      PROGRAM_TO_INSTRUMENT[program] = instrument;
    }
  }

  private static void put(int program, ScInstrument instrument) {
    PROGRAM_TO_INSTRUMENT[program] = instrument;
  }

  private static void drum(int scDrum, int... gmNotes) {
    for (int note : gmNotes) {
      PERCUSSION_NOTE_TO_DRUM.put(note, scDrum);
    }
  }

  /**
   * 获取通道+音色对应的 SC 乐器
   *
   * @param channel MIDI 通道(0~15)
   * @param program GM 音色(0~127)
   * @return SC 乐器, 不支持时返回 null
   */
  public static ScInstrument instrumentOf(int channel, int program) {
    if (channel == PERCUSSION_CHANNEL) {
      return ScInstrument.DRUMS;
    }
    if (program < 0 || program >= PROGRAM_TO_INSTRUMENT.length) {
      return null;
    }
    return PROGRAM_TO_INSTRUMENT[program];
  }

  /**
   * 获取鼓声编号(即鼓的 P 端取值)
   *
   * @param channel MIDI 通道
   * @param program GM 音色
   * @param note MIDI 音符号
   * @return 0~9, 没有合适的鼓声时返回 null
   */
  public static Integer drumPitchOf(int channel, int program, int note) {
    if (channel == PERCUSSION_CHANNEL) {
      return PERCUSSION_NOTE_TO_DRUM.get(note);
    }
    return PERCUSSIVE_PROGRAM_TO_DRUM.get(program);
  }

  /**
   * GM 打击乐音符对应的鼓声名
   *
   * @param note MIDI 音符号
   * @return 名称
   */
  public static String percussionNoteName(int note) {
    return InstrumentInfo.getSpecialChannelInstrumentNameByCode(note);
  }
}
