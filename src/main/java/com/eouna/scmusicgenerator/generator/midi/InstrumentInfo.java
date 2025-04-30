package com.eouna.scmusicgenerator.generator.midi;

import java.util.HashMap;
import java.util.Map;

/**
 * 乐器信息
 *
 * @author CCL
 */
public class InstrumentInfo {
  /** 乐器名 */
  private static final Map<Integer, String> MIDI_CODE_TO_INSTRUMENT_NAME = new HashMap<>();

  /** 特殊通道的乐器名 列如通道10 */
  private static final Map<Integer, String> SPECIAL_CHANNEL_INSTRUMENT_NAME = new HashMap<>();

  /** 功能控制 */
  private static final Map<Integer, String> FUNCTION_CONTROL_NAME = new HashMap<>();

  static {
    // 钢琴
    MIDI_CODE_TO_INSTRUMENT_NAME.put(1, "Acoustic Grand Piano	平台钢琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(2, "Bright Acoustic Piano	亮音钢琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(3, "Electric Grand Piano	电钢琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(4, "Honky-tonk Piano	酒吧钢琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(5, "Electric Piano 1	电钢琴1");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(6, "Electric Piano 2	电钢琴2");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(7, "Harpsichord	大键琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(8, "Clavinet	电翼琴");
    // Chromatic Percussion（固定音高敲击乐器）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(9, "Celesta	钢片琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(10, "Glockenspiel	钟琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(11, "Musical box	音乐盒");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(12, "Vibraphone	颤音琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(13, "Marimba	马林巴琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(14, "Xylophone	木琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(15, "Tubular Bell	管钟");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(16, "Dulcimer	洋琴");
    // Organ（风琴）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(17, "Drawbar Organ	音栓风琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(18, "Percussive Organ	敲击风琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(19, "Rock Organ	摇滚风琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(20, "Church organ	教堂管风琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(21, "Reed organ	簧风琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(22, "Accordion	手风琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(23, "Harmonica	口琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(24, "Tango Accordion	探戈手风琴");
    // Guitar（吉他）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(25, "Acoustic Guitar(nylon)	木吉他（尼龙弦）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(26, "Acoustic Guitar(steel)	木吉他（钢弦）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(27, "Electric Guitar(jazz)	电吉他（爵士）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(28, "Electric Guitar(clean)	电吉他（原音）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(29, "Electric Guitar(muted)	电吉他（闷音）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(30, "Overdriven Guitar	电吉他（破音）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(31, "Distortion Guitar	电吉他（失真）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(32, "Guitar harmonics	吉他泛音");
    // Bass（贝斯）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(33, "Acoustic Bass	民谣贝斯");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(34, "Electric Bass(finger)	电贝斯（指奏）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(35, "Electric Bass(pick)	电贝斯（拨奏）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(36, "Fretless Bass	无格贝斯");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(37, "Slap Bass 1	捶钩贝斯 1");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(38, "Slap Bass 2	捶钩贝斯 2");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(39, "Synth Bass 1	合成贝斯1");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(40, "Synth Bass 2	合成贝斯2");
    // Strings（弦乐 器）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(41, "Violin	小提琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(42, "Viola	中提琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(43, "Cello	大提琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(44, "Contrabass	低音大提琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(45, "Tremolo Strings	颤弓弦乐");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(46, "Pizzicato Strings	弹拨弦乐");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(47, "Orchestral Harp	竖琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(48, "Timpani	定音鼓");
    // Ensemble（合奏）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(49, "String Ensemble 1	弦乐合奏1");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(50, "String Ensemble 2	弦乐合奏2");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(51, "Synth Strings 1	合成弦乐1");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(52, "Synth Strings 2	合成弦乐2");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(53, "Voice Aahs	人声“啊”");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(54, "Voice Oohs	人声“喔”");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(55, "Synth Voice	合成人声");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(56, "Orchestra Hit	交响打击乐");
    // Brass（铜管 乐器）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(57, "Trumpet	小号");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(58, "Trombone	长号");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(59, "Tuba	大号（吐巴号、低音号）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(60, "Muted Trumpet	闷音小号");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(61, "French horn	法国号（圆号）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(62, "Brass Section	铜管乐");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(63, "Synth Brass 1	合成铜管1");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(64, "Synth Brass 2	合成铜管2");
    // Reed（簧乐 器）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(65, "Soprano Sax	高音萨克斯风");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(66, "Alto Sax	中音萨克斯风");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(67, "Tenor Sax	次中音萨克斯风");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(68, "Baritone Sax	上低音萨克斯风");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(69, "Oboe	双簧管");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(70, "English Horn	英国管");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(71, "Bassoon	低音管（巴颂管）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(72, "Clarinet	单簧管（黑管、竖笛）");
    // Pipe（吹管 乐器）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(73, "Piccolo	短笛");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(74, "Flute	长笛");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(75, "Recorder	直笛");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(76, "Pan Flute	排箫");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(77, "Blown Bottle	瓶笛");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(78, "Shakuhachi	尺八");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(79, "Whistle	哨子");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(80, "Ocarina	陶笛");
    // Synth Lead（合成音主旋律）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(81, "Lead 1(square)	方波");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(82, "Lead 2(sawtooth)	锯齿波");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(83, "Lead 3(calliope)	汽笛风琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(84, "Lead 4(chiff)	合成吹管");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(85, "Lead 5(charang)	合成电吉他");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(86, "Lead 6(voice)	人声键盘");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(87, "Lead 7(fifths)	五度音");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(88, "Lead 8(bass + lead)	贝斯吉他合奏");
    // Synth Pad（合成音和弦衬底）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(89, "Pad 1(new age)	新世纪");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(90, "Pad 2(warm)	温暖");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(91, "Pad 3(polysynth)	多重合音");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(92, "Pad 4(choir)	人声合唱");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(93, "Pad 5(bowed)	玻璃");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(94, "Pad 6(metallic)	金属");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(95, "Pad 7(halo)	光华");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(96, "Pad 8(sweep)	扫掠");
    // Synth Effects（合成音效果）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(97, "FX 1(rain)	雨");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(98, "FX 2(soundtrack)	电影音效");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(99, "FX 3(crystal)	水晶");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(100, "FX 4(atmosphere)	气氛");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(101, "FX 5(brightness)	明亮");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(102, "FX 6(goblins)	魅影");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(103, "FX 7(echoes)	回音");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(104, "FX 8(sci-fi)	科幻");
    // Ethnic（民族 乐器）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(105, "Sitar	西塔琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(106, "Banjo	五弦琴（斑鸠琴）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(107, "Shamisen	三味线");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(108, "Koto	十三弦琴（古筝）");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(109, "Kalimba	卡林巴铁片琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(110, "Bagpipe	苏格兰风笛");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(111, "Fiddle	古提琴");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(112, "Shanai	印度唢呐");
    // Percussive（打击 乐器）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(113, "Tinkle Bell	叮当铃");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(114, "Agogo	阿哥哥鼓");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(115, "Steel Drums	钢鼓");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(116, "Woodblock	木鱼");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(117, "Taiko Drum	太鼓");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(118, "Melodic Tom	定音筒鼓");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(119, "Synth Drum	合成鼓");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(120, "Reverse Cymbal	逆转钹声");
    // Sound effects（特殊 音效）
    MIDI_CODE_TO_INSTRUMENT_NAME.put(121, "Guitar Fret Noise	吉他滑弦杂音");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(122, "Breath Noise	呼吸杂音");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(123, "Seashore	海岸");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(124, "Bird Tweet	鸟鸣");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(125, "Telephone Ring	电话铃声");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(126, "Helicopter	直升机");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(127, "Applause	拍手");
    MIDI_CODE_TO_INSTRUMENT_NAME.put(128, "Gunshot	枪声");
    // 打击乐音符（Percussion notes）
    // 在General MIDI中，频道10,"保留作为打击乐器使用，不论音色编号为何。不同的音符对应到不同的打击乐器。见下表：";

    // No.	English	中文
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(35, "Bass Drum 2	大鼓2");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(36, "Bass Drum 1	大鼓1");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(37, "Side Stick	小鼓鼓边");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(38, "Snare Drum 1	小鼓1");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(39, "Hand Clap	拍手");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(40, "Snare Drum 2	小鼓2");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(41, "Low Tom 2	低音筒鼓2");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(42, "Closed Hi-hat	闭合开合钹");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(43, "Low Tom 1	低音筒鼓1");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(44, "Pedal Hi-hat	脚踏开合钹");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(45, "Mid Tom 2	中音筒鼓2");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(46, "Open Hi-hat	开放开合钹");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(47, "Mid Tom 1	中音筒鼓1");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(48, "High Tom 2	高音筒鼓2");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(49, "Crash Cymbal 1	强音钹1");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(50, "High Tom 1	高音筒鼓1");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(51, "Ride Cymbal 1	打点钹1");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(52, "Chinese Cymbal	钹");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(53, "Ride Bell	响铃");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(54, "Tambourine	铃鼓");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(55, "Splash Cymbal	小钹铜钹");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(56, "Cowbell	牛铃");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(57, "Crash Cymbal 2	强音钹2");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(58, "Vibra Slap	噪音器");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(59, "Ride Cymbal 2	打点钹2");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(60, "High Bongo	高音邦加鼓");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(61, "Low Bongo	低音邦加鼓");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(62, "Mute High Conga	闷音高音康加鼓");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(63, "Open High Conga	开放高音康加鼓");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(64, "Low Conga	低音康加鼓");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(65, "High Timbale	高音天巴雷鼓");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(66, "Low Timbale	低音天巴雷鼓");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(67, "High Agogo	高音阿哥哥铃");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(68, "Low Agogo	低音阿哥哥铃");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(69, "Cabasa	铁沙铃");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(70, "Maracas	沙槌");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(71, "Short Whistle	短口哨");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(72, "Long Whistle	长口哨");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(73, "Short Guiro	短刮瓜");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(74, "Long Guiro	长刮瓜");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(75, "Claves	击木");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(76, "High Wood Block	高音木鱼");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(77, "Low Wood Block	低音木鱼");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(78, "Mute Cuica");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(79, "Open Cuica");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(80, "Mute Triangle	闷音三角铁");
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(81, "Open Triangle	开放三角铁");
    // 控制器事件（Controller events）
    SPECIAL_CHANNEL_INSTRUMENT_NAME.put(1, "]");

    // No.	功能
    FUNCTION_CONTROL_NAME.put(1, "Modulation（颤音）");
    FUNCTION_CONTROL_NAME.put(6, "Data Entry MSB");
    FUNCTION_CONTROL_NAME.put(7, "Volume（音量）");
    FUNCTION_CONTROL_NAME.put(10, "Pan（相位）");
    FUNCTION_CONTROL_NAME.put(11, "Expression（表情踏板）");
    FUNCTION_CONTROL_NAME.put(38, "Data Entry LSB");
    FUNCTION_CONTROL_NAME.put(64, "Sustain（延音踏板）");
    FUNCTION_CONTROL_NAME.put(100, "RPN LSB");
    FUNCTION_CONTROL_NAME.put(101, "RPN MSB");
    FUNCTION_CONTROL_NAME.put(121, "Reset all controllers（重设所有控制器）");
    FUNCTION_CONTROL_NAME.put(123, "All notes off（消音）");
  }

  public static String getFunctionControlNameByCode(int code) {
    return FUNCTION_CONTROL_NAME.getOrDefault(code, "未知");
  }

  public static String getInstrumentNameByCode(int code) {
    return MIDI_CODE_TO_INSTRUMENT_NAME.getOrDefault(code, "未知乐器");
  }

  public static String getSpecialChannelInstrumentNameByCode(int code) {
    return SPECIAL_CHANNEL_INSTRUMENT_NAME.getOrDefault(code, "未知");
  }
}
