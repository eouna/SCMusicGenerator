package com.eouna.scmusicgenerator.generator.midi;

/**
 * 从 MIDI 文件中解析出的一次按键(NOTE_ON, velocity > 0).
 *
 * <p>生存战争的发音器一旦触发就按采样自然衰减, 无法控制时值, 因此这里不保留 NOTE_OFF.
 *
 * @author CCL
 */
public final class MidiNote {
  /** 绝对 tick */
  public final long tick;

  /** MIDI 通道 0~15 */
  public final int channel;

  /** 按下时该通道的 GM 音色(0~127) */
  public final int program;

  /** MIDI 音符号 0~127 */
  public final int note;

  /** 力度 1~127 */
  public final int velocity;

  /** 映射后的 SC 乐器 */
  public final ScInstrument instrument;

  public MidiNote(
      long tick, int channel, int program, int note, int velocity, ScInstrument instrument) {
    this.tick = tick;
    this.channel = channel;
    this.program = program;
    this.note = note;
    this.velocity = velocity;
    this.instrument = instrument;
  }

  @Override
  public String toString() {
    return "MidiNote{tick="
        + tick
        + ", channel="
        + channel
        + ", program="
        + program
        + ", note="
        + note
        + ", velocity="
        + velocity
        + ", instrument="
        + instrument
        + '}';
  }
}
