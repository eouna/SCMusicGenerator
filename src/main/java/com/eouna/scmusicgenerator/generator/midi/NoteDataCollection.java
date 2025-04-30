package com.eouna.scmusicgenerator.generator.midi;

import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

/**
 * 乐器收集
 *
 * @author CCL
 */
public class NoteDataCollection {
  /** 当前乐器里的最高八度 用来适配SC中的八度尽量减少音差 */
  private int maxOctave = Integer.MIN_VALUE;

  /** 当前乐器里的最低八度 */
  private int minOctave = Integer.MAX_VALUE;

  /** 最大的响度，如果当前音乐最大响度不满，直接将所有响度提高 */
  private int maxVelocity = Integer.MIN_VALUE;

  /** tick序列 */
  private final TreeMap<Long, List<ScNoteData>> noteData = new TreeMap<>();

  /** 通道 */
  private final HashSet<Integer> channelSet = new HashSet<>();

  public NoteDataCollection(int firstChannel) {
    channelSet.add(firstChannel);
  }

  public int getMaxOctave() {
    return maxOctave;
  }

  public void setMaxOctave(int maxOctave) {
    this.maxOctave = maxOctave;
  }

  public int getMinOctave() {
    return minOctave;
  }

  public int getMaxVelocity() {
    return maxVelocity;
  }

  public void setMinOctave(int minOctave) {
    this.minOctave = minOctave;
  }

  public TreeMap<Long, List<ScNoteData>> getNoteData() {
    return noteData;
  }

  /** 同步当前最高八度和最低八度 */
  public void updateNoteInfoData(int octave, int analogVel) {
    minOctave = Math.min(minOctave, octave);
    maxOctave = Math.max(maxVelocity, octave);
    maxVelocity = Math.max(maxVelocity, analogVel);
  }

  /** 同步当前最高八度和最低八度 */
  public void updateChannel(int channel) {
    channelSet.add(channel);
  }

  public HashSet<Integer> getChannelSet() {
    return channelSet;
  }

  public static class ScNoteData {
    // 音高
    public int pitch;
    // 响度
    public int velocity;
    // 八度
    public int octaves;

    public ScNoteData() {}

    public ScNoteData(int pitch, int velocity, int octaves) {
      this.pitch = pitch;
      this.velocity = velocity;
      this.octaves = octaves;
    }

    @Override
    public String toString() {
      return "ScNoteData{"
          + "pitch="
          + pitch
          + ", velocity="
          + velocity
          + ", octaves="
          + octaves
          + '}';
    }
  }
}
