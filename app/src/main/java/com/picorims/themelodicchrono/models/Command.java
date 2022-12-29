package com.picorims.themelodicchrono.models;

import android.util.Log;

import java.util.ArrayList;

public class Command {
    public static final String TAG = "Command";
    public enum CommandTypes {
        AT,
        EVERY
    };
    public enum PlayModeTypes {
        SCALE,
        ARPEGGIO,
        REPEAT
    }
    private CommandTypes commandType;
    private long timestamp;
    private ArrayList<String> notes;
    private PlayModeTypes playMode;
    private int repeatModeMax;
    private int cursor; // keep track of where we are for scale, arpeggio, repeat...

    /**
     * Creates a new command based on the given parameters.
     * @param commandType the base command name
     * @param timestamp the reference from zero that tells when to play notes
     * @param notes the notes to play
     * @param playMode the mode to use to play notes
     * @param repeatModeMax if in REPEAT mode, the max number of repeats.
     *                      It is set to -1 if not in repeat mode.
     *                      If in REPEAT mode and equals to -1, it defaults to 1.
     */
    public Command(CommandTypes commandType, long timestamp, ArrayList notes, PlayModeTypes playMode, int repeatModeMax) {
        if (commandType == null) throw new IllegalArgumentException("the command type must be specified");
        if (playMode == null) throw new IllegalArgumentException("the play mode type must be specified");
        this.commandType = commandType;
        this.timestamp = timestamp;
        this.notes = notes;
        this.playMode = playMode;
        this.repeatModeMax = (playMode == PlayModeTypes.REPEAT)? repeatModeMax : -1;
        if (this.repeatModeMax == -1 && playMode == PlayModeTypes.REPEAT) this.repeatModeMax = 1;
        cursor = 0;
    }

    /**
     * A short version of the Command constructor that defaults to the SCALE mode.
     * For AT commands, this constructor is ideal as the play mode is ignored (it can play
     * only once, thus it is irrelevant).
     * @param commandType
     * @param timestamp
     * @param notes
     */
    public Command(CommandTypes commandType, long timestamp, ArrayList notes) {
        this(commandType, timestamp, notes, PlayModeTypes.SCALE, -1);
    }

    /**
     * Gives the notes that should be played within two given timestamps.
     * The function assumes we start from zero. In other words, the timestamps
     * should be based on when the chrono started, rather than being a UNIX timestamp.
     * @param timestampMin
     * @param timestampMax
     * @return
     */
    public ArrayList<String> notesToPlayBetween(long timestampMin, long timestampMax) {
        if (commandType == CommandTypes.AT) {
            // AT
            if (timestamp >= timestampMin && timestamp < timestampMax) {
                // there is no mode for at, we just play all the listed notes.
                return (ArrayList<String>) notes.clone();
            }
        } else if (commandType == CommandTypes.EVERY) {
            // EVERY
            long minMod = timestampMin % timestamp;
            long maxMod = timestampMax % timestamp;
            // We use modulo to know when to repeat regardless of how much time ellapsed.
            // To repeat every 5s, we look for (time_ellapsed % 5s) = 0.
            // 5s, 10s, 15s all leads to 0.
            //
            // if min > max, then min didn't loop but max did.
            // That means 0 modulo timestamp is in the range.
            // In other words the moment to repeat the event again is now.
            if (minMod > maxMod) {
                return (ArrayList<String>) getNotes();
            }
        }
        //nothing to send otherwise
        return new ArrayList<>();
    }

    /**
     * Returns the list of notes to play based on the cursor position and the play mode.
     * It automatically increments the cursor as well!
     * @return
     */
    private ArrayList<String> getNotes() {
        ArrayList<String> notesToReturn = new ArrayList<>();
        if (playMode == PlayModeTypes.SCALE) {
            //SCALE
            notesToReturn.add(notes.get(cursor));
            cursor = (cursor+1) % notes.size();

        } else if (playMode == PlayModeTypes.ARPEGGIO) {
            //ARPEGGIO
            notesToReturn.addAll(notes.subList(0, cursor));
            cursor = (cursor+1) % notes.size();

        } else if (playMode == PlayModeTypes.REPEAT) {
            //REPEAT
            for (int i = 0; i < cursor+1; i++) {
                notesToReturn.addAll(notes);
            }
            cursor = (cursor+1) % repeatModeMax;

        }
        return notesToReturn;
    }
}
