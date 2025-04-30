package com.eouna.scmusicgenerator.generator.mp3;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.eouna.scmusicgenerator.utils.FileUtils;
import com.eouna.scmusicgenerator.utils.MainWindowTextAreaLogger;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.*;
import javax.sound.sampled.*;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;

/**
 * Mp3转Midi工具
 *
 * @author CCL
 */
public class Mp3ToMidiDecoder {
  /** 采样率 */
  private static final int SAMPLE_RATE = 44100;

  private static final int BUFFER_SIZE = 1024;
  private static final int OVERLAP = 512;

  /** 小于50ms的忽略 */
  private static final int MIN_NOTE_DURATION_MS = 50;

  /** MIDI 精度 */
  private static final int PPQ = 480;

  /** 假设速度120BPM */
  private static final int BPM = 120;

  static class NoteEvent {
    int midiNote;
    long startTick;
    long endTick;

    public NoteEvent(int midiNote, long startTick) {
      this.midiNote = midiNote;
      this.startTick = startTick;
    }
  }

  /**
   * 调用第三方的MP3转MIDI方法
   *
   * @param logger logger
   * @param mp3File mp3文件
   * @return 转换后的MIDI文件
   * @throws IOException e
   */
  public static File decodeMp3ToMidiFromApi(MainWindowTextAreaLogger logger, File mp3File)
      throws IOException {
    File baseTmpFileDir = new File(FileUtils.getRootDir() + "./music/convert_tmp/");
    checkMusicGenTempDir();
    File targetMidiFile =
        new File(
            baseTmpFileDir.getAbsolutePath()
                + File.separator
                + (mp3File.getName().substring(0, mp3File.getName().lastIndexOf(".mp3")))
                + ".mid");
    // 写成配置文件
    String requestUri = "https://api.ai-midi.com/predict";
    try (CloseableHttpClient httpClients = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(requestUri);
      MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
      multipartEntityBuilder.addBinaryBody("input_audio", mp3File);
      multipartEntityBuilder.addTextBody("bpm", "120");
      multipartEntityBuilder.addTextBody("beat", "4");
      multipartEntityBuilder.addTextBody("bar", "4");
      httpPost.setEntity(multipartEntityBuilder.build());
      return httpClients.execute(
          httpPost,
          new AbstractHttpClientResponseHandler<>() {
            @Override
            public File handleEntity(HttpEntity httpEntity) {
              try {
                InputStream inputStream = httpEntity.getContent();
                FileOutputStream fileOutputStream = new FileOutputStream(targetMidiFile);
                byte[] bufferResponse = new byte[4096];
                int bytesReadResponse;
                while ((bytesReadResponse = inputStream.read(bufferResponse)) != -1) {
                  fileOutputStream.write(bufferResponse, 0, bytesReadResponse);
                }
                logger.success(
                    "文件: {} 转换完成!文件路径：{}",
                    targetMidiFile.getName(),
                    targetMidiFile.getAbsolutePath());
                fileOutputStream.close();
                inputStream.close();
                return targetMidiFile;
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          });
    }
  }

  /**
   * 将MP3转为midi文件
   *
   * @param mp3File MP3文件
   * @return midi文件
   * @throws Exception e
   */
  public static File decodeMp3ToMidiFile(File mp3File) throws Exception {
    return decodeMp3ToMidiFile(mp3File, SAMPLE_RATE, PPQ, BPM);
  }

  private static void checkMusicGenTempDir() {
    File baseTmpFileDir = new File(FileUtils.getRootDir() + "./music/convert_tmp/");
    if (!baseTmpFileDir.exists()) {
      if (!baseTmpFileDir.mkdirs()) {
        throw new RuntimeException("创建临时文件夹" + baseTmpFileDir.getAbsolutePath() + "失败");
      }
    }
  }

  /**
   * 将MP3转为midi文件
   *
   * @param mp3File MP3文件
   * @param simpleRate 采样率
   * @param ppq 每4分音符tick数
   * @param bpm 每分钟的拍子数 beat per minute
   * @return midi文件
   * @throws Exception e
   */
  public static File decodeMp3ToMidiFile(File mp3File, int simpleRate, int ppq, int bpm)
      throws Exception {
    File baseTmpFileDir = new File(FileUtils.getRootDir() + "./music/convert_tmp/");
    checkMusicGenTempDir();
    // 1.先将MP3文件转为WAV文件
    File wavFile = convertMp3ToWav(mp3File);
    // 2.解析midi文件
    List<NoteEvent> midiNoteEventList = extractNotes(wavFile, simpleRate, ppq, bpm);
    // midi最终文件
    File midiFile =
        new File(
            baseTmpFileDir.getAbsolutePath()
                + File.separator
                + mp3File.getName().substring(0, mp3File.getName().indexOf(".mp3"))
                + ".mid");
    // 3.保存midi文件
    saveAsMidi(midiNoteEventList, midiFile, ppq);
    return midiFile;
  }

  /**
   * 将MP3文件转为WAV文件
   *
   * @param mp3File MP3
   * @return wav
   * @throws UnsupportedAudioFileException e
   * @throws IOException e
   */
  public static File convertMp3ToWav(File mp3File)
      throws UnsupportedAudioFileException, IOException {
    AudioInputStream mp3Audio = AudioSystem.getAudioInputStream(mp3File);
    AudioFormat baseFormat = mp3Audio.getFormat();

    AudioFormat decodedFormat =
        new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.getSampleRate(),
            16,
            baseFormat.getChannels(),
            baseFormat.getChannels() * 2,
            baseFormat.getSampleRate(),
            false);

    AudioInputStream decodedAudio = AudioSystem.getAudioInputStream(decodedFormat, mp3Audio);

    File wavFile = new File("./music/convert_tmp/converted.wav");
    AudioSystem.write(decodedAudio, AudioFileFormat.Type.WAVE, wavFile);
    return wavFile;
  }

  /**
   * 从wav文件解码
   *
   * @param file wav文件
   * @return midi note
   */
  private static List<NoteEvent> extractNotes(File file, int simpleRate, int ppq, int bpm) {
    List<NoteEvent> noteEvents = new ArrayList<>();

    AudioDispatcher dispatcher =
        AudioDispatcherFactory.fromPipe(file.getAbsolutePath(), simpleRate, BUFFER_SIZE, OVERLAP);

    PitchDetectionHandler handler =
        new PitchDetectionHandler() {
          private float lastPitch = -1;
          private long lastTimeMs = 0;
          private NoteEvent currentNote = null;

          @Override
          public void handlePitch(PitchDetectionResult result, AudioEvent audioEvent) {
            float pitch = result.getPitch();
            long timeMs = (long) (audioEvent.getTimeStamp() * 1000);

            if (isSimilarPitch(pitch, lastPitch)) {
              // 连续的，继续
              return;
            } else {
              if (currentNote != null) {
                long duration = timeMs - lastTimeMs;
                if (duration >= MIN_NOTE_DURATION_MS) {
                  currentNote.endTick = msToTick(timeMs, ppq, bpm);
                  noteEvents.add(currentNote);
                }
                currentNote = null;
              }

              if (pitch != -1) {
                int midiNote = freqToMidi(pitch);
                currentNote = new NoteEvent(midiNote, msToTick(timeMs, ppq, bpm));
              }
            }

            lastPitch = pitch;
            lastTimeMs = timeMs;
          }
        };

    dispatcher.addAudioProcessor(
        new PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, simpleRate, BUFFER_SIZE, handler));

    dispatcher.run();
    return noteEvents;
  }

  private static boolean isSimilarPitch(float p1, float p2) {
    if (p1 == -1 || p2 == -1) {
      return false;
    }
    // 2Hz以内算同一个音
    return Math.abs(p1 - p2) < 2.0f;
  }

  private static int freqToMidi(float freq) {
    if (freq <= 0) {
      return -1;
    }
    return Math.round(69 + 12 * (float) (Math.log(freq / 440.0) / Math.log(2)));
  }

  private static long msToTick(long ms, int ppq, int bpm) {
    return (ms * ppq * bpm) / (60 * 1000);
  }

  /**
   * 保存midi数据到文件
   *
   * @param notes midi数据
   * @param midiFile file
   * @throws Exception e
   */
  private static void saveAsMidi(List<NoteEvent> notes, File midiFile, int ppq) throws Exception {
    Sequence sequence = new Sequence(Sequence.PPQ, ppq);
    Track track = sequence.createTrack();

    for (NoteEvent note : notes) {
      ShortMessage on = new ShortMessage();
      on.setMessage(ShortMessage.NOTE_ON, 0, note.midiNote, 100);
      track.add(new MidiEvent(on, note.startTick));

      ShortMessage off = new ShortMessage();
      off.setMessage(ShortMessage.NOTE_OFF, 0, note.midiNote, 0);
      track.add(new MidiEvent(off, note.endTick));
    }

    MidiSystem.write(sequence, 1, midiFile);
  }
}
