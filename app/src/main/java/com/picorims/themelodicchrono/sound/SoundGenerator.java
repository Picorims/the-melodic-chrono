package com.picorims.themelodicchrono.sound;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;

/**
 * Class that generates sinusoides.
 * Source:
 * - https://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
 * - https://stackoverflow.com/a/23399026
 */
public class SoundGenerator {
    public static final String TAG = "MelodicChrono::SoundGenerator";
    private static final HashMap<String, Integer> NOTES = new HashMap<>();

    static {
        NOTES.put("C", 0);
        NOTES.put("D", 2);
        NOTES.put("E", 4);
        NOTES.put("F", 5);
        NOTES.put("G", 7);
        NOTES.put("A", 9);
        NOTES.put("B", 11);
    }

    public static void playNote(String note, double duration) {
        //find index of the note relative to A4 = 0
        int C4 = -9;
        int noteIndex = C4;
        String baseNote = note.substring(0,1);
        String modifier = (note.length() == 2)? note.substring(1,2) : "";
        if (modifier == "#") noteIndex++;
        if (modifier == "b") noteIndex--;
        noteIndex += NOTES.get(note);

        // *2 = +1 octave so 12, 24, 36... are octaves.
        // there are twelve semitones in an octave, so we divide by 12 to access them all.
        // based on A 440.
        // 440 * 2^(noteIndex/12)
        double noteHz = (440 * Math.pow(2, ((double) noteIndex)/12));

        Log.d(TAG, "playNote: " + noteHz + " " + noteIndex);

        playTone(noteHz, duration);
    }
    public static void playTone(double freqOfTone, double duration) {
        // Use a new tread as this can take a while
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                playSound(freqOfTone, duration);
            }
        });
        thread.start();
    }
    private static void playSound(double freqOfTone, double duration) {
        //double duration = 1000;                // seconds
        //   double freqOfTone = 1000;           // hz
        int sampleRate = 8000;              // a number

        double dnumSamples = duration * sampleRate;
        dnumSamples = Math.ceil(dnumSamples);
        int numSamples = (int) dnumSamples;
        double sample[] = new double[numSamples];
        byte generatedSnd[] = new byte[2 * numSamples];


        for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
            sample[i] = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalized.
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        int i = 0 ;

        int ramp = numSamples / 20 ;                                    // Amplitude ramp as a percent of sample count

        for (i = 0; i< ramp; ++i) {                                     // Ramp amplitude up (to avoid clicks)
            double dVal = sample[i];
            // Ramp up to maximum
            final short val = (short) ((dVal * 32767 * i/ramp));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }


        for (i = i; i< numSamples - ramp; ++i) {                        // Max amplitude for most of the samples
            double dVal = sample[i];
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        for (i = i; i< numSamples; ++i) {                               // Ramp amplitude down
            double dVal = sample[i];
            // Ramp down to zero
            final short val = (short) ((dVal * 32767 * (numSamples-i)/ramp ));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        AudioTrack audioTrack = null;                                   // Get audio track
        try {
            int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                    AudioTrack.MODE_STREAM);
            audioTrack.setVolume(0.5f);
            audioTrack.play();                                          // Play the track
            audioTrack.write(generatedSnd, 0, generatedSnd.length);     // Load the track
        }
        catch (Exception e){
        }
        if (audioTrack != null) audioTrack.release();           // Track play done. Release track.
    }
}
