package com.picorims.themelodicchrono.sound;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
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
    public static final String TAG = "SoundGenerator";
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

    /**
     * Play the given note for the given duration
     * @param note
     * @param duration
     * @param activity activity for which we use the audio service
     */
    public static void playNote(String note, double duration, long delay, Activity activity) {
        //find index of the note relative to A4 = 0
        int C4 = -9;
        int noteIndex = C4;
        String baseNote = note.substring(0,1);
        String modifier = (note.length() == 2)? note.substring(1,2) : "";
        if (modifier.equals("#")) noteIndex++;
        if (modifier.equals("b")) noteIndex--;
        noteIndex += NOTES.get(baseNote);

        // *2 = +1 octave so 12, 24, 36... are octaves.
        // there are twelve semitones in an octave, so we divide by 12 to access them all.
        // based on A 440.
        // 440 * 2^(noteIndex/12)
        double noteHz = (440 * Math.pow(2, ((double) noteIndex)/12));

        Log.d(TAG, "playNote: " + noteHz + " " + noteIndex);

        playTone(noteHz, duration, delay, activity);
    }

    /**
     * Play the given frequency for the given duration in a separate thread
     * @param freqOfTone
     * @param duration
     * @param activity activity for which we use the audio service
     */
    public static void playTone(double freqOfTone, double duration, long delay, Activity activity) {
        // Use a new tread as this can take a while
        final Runnable noteRunnable = new Runnable() {
            public void run() {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        playSound(freqOfTone, duration, activity);
                    }
                });
                t.start();
            }
        };

        if (delay == 0) noteRunnable.run();
        else new Handler().postDelayed(noteRunnable, delay);
    }

    /**
     * Plays the given frequency for the given duration
     * @param freqOfTone
     * @param duration
     * @param activity activity for which we use the audio service
     */
    private static void playSound(double freqOfTone, double duration, Activity activity) {
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

            //create audio
            AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();

            // initialize with new constructor
            audioTrack = new AudioTrack(audioAttributes,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    0);

            //DEPRECATED =====
//            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
//                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
//                    AudioFormat.ENCODING_PCM_16BIT, bufferSize,
//                    AudioTrack.MODE_STREAM);
            // ===============
            //play audio
            audioTrack.setVolume(0.5f);
            audioTrack.play();                                          // Play the track
            audioTrack.write(generatedSnd, 0, generatedSnd.length);     // Load the track
        }
        catch (Exception e){
        }
        if (audioTrack != null) audioTrack.release();           // Track play done. Release track.
    }
}
