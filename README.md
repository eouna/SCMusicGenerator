<h1 align="center">SurvivalCraft Music Data Generator</h1> 
<img alt="logo" src="src/main/resources/com/eouna/scmusicgenerator/icon/main.png">

English|[中文](./doc/中文.md)

# SCMusicGenerator(SurvivalCraft Music Data Generator)

The tool is designed to convert MIDI files into music data usable in Survivalcraft. It tells you how many music plates you’ll need and, for each plate, provides the required parameters—volume, octave, instrument code, and pitch data.

Usage is very simple: load your MIDI file into the tool, pick a quantization step and click “Generate.” If you start with an MP3, first convert it to MIDI before running the generator.

# Conversion rules

- **Unified time grid**: one memory-bank cell = one quantization step (default 1/4 beat; 1/8 beat … 1 beat or a fixed number of milliseconds can be selected in the UI). All instruments share the same timeline, cell N is step N of the song, and a long note is one sounding cell followed by rest cells (P=F, V=0). The log prints the **clock period** (seconds) you should use for the circuit counter.
- **Voices**: several notes of the same instrument in the same cell are spread over several sound generators (Piano1, Piano2, …), so the number of generators equals the maximum polyphony. Every voice gets 4 memory banks per page, and page N of every voice is time-aligned.
- **Pitch**: absolute mapping using the game formula `MIDI note = 48 + 12 × O + P` (octave 0 = C3, octave 1 = the middle-C octave); Organ supports octaves 0–2, the other instruments 0–3. Songs outside the range are transposed by whole octaves, remaining notes are folded back into range (see the log).
- **Instruments**: GM programs are mapped to Bell/Organ/Ping/String/Trumpet/Voice/Piano/PianoSustain/Bass; channel 10 is always drums and GM percussion notes are mapped to the 10 in-game drum sounds (0 Snare 1 Bass drum 2 Closed hi-hat 3 Pedal hi-hat 4 Open hi-hat 5 Low tom 6 High tom 7 Crash 8 Ride 9 Hand clap).
- **The tone input is the trigger**: a sound generator only plays when its tone input goes from 0 to non-zero. In the default “constant tone” mode the tone memory bank must be ANDed with the clock pulse (level F); the “pulse” mode inserts a 0 cell after every note so no AND gate is needed, at the cost of 128 steps per bank.
- **Volume**: `V = round(velocity / 127 × 15)`; with “normalize velocity” checked the loudest note of the song becomes F.
- **Preview**: after generating, click “Preview” to hear the result. The tool simulates every sound generator cell by cell from the generated memory-bank data (clock period, tone rising-edge trigger, 0.08 s minimum retrigger interval, pitch/volume formulas); all voices play together in rhythm and the data panel follows the page being played. The preview uses the built-in MIDI synthesizer (not the game samples), so use it to check rhythm, harmony and range.

# Tool interface

1. Main Interface
   ![Main Interface](doc/img/main.png)

2. Describe Interface
   ![Describe Interface](doc/img/describe.png)

3. After Generator
   ![After Generator](doc/img/after_gen.png)
