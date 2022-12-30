/*
MIT License

Copyright (c) 2022 Picorims

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.picorims.themelodicchrono.models;

import android.util.Log;
import android.util.Pair;

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
    private Rules rules;
    private CommandTypes commandType;
    private long timestamp;
    private ArrayList<String> notes;
    private PlayModeTypes playMode;
    private int repeatModeMax;
    private int cursor; //position in the scale, arpeggio, number of repetitions, etc.

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
    public Command(Rules rules, CommandTypes commandType, long timestamp, ArrayList notes, PlayModeTypes playMode, int repeatModeMax) {
        if (commandType == null) throw new IllegalArgumentException("the command type must be specified");
        if (playMode == null) throw new IllegalArgumentException("the play mode type must be specified");
        this.rules = rules;
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
    public Command(Rules rules, CommandTypes commandType, long timestamp, ArrayList notes) {
        this(rules, commandType, timestamp, notes, PlayModeTypes.REPEAT, 1);
    }

    /**
     * Gives the notes that should be played within two given timestamps.
     * The function assumes we start from zero. In other words, the timestamps
     * should be based on when the chrono started, rather than being a UNIX timestamp.
     * @param timestampMin
     * @param timestampMax
     * @return
     */
    public ArrayList<Pair<String, Long>> notesToPlayBetween(long timestampMin, long timestampMax) {
        ArrayList<Pair<String, Long>> notesToSend = null;
        if (commandType == CommandTypes.AT) {
            // AT
            if (timestamp >= timestampMin && timestamp < timestampMax) {
                // there is no mode for at, we just play all the listed notes.
                notesToSend = new ArrayList<>();
                for (int i = 0; i < notes.size(); i++) {
                    notesToSend.add(delayedNote(notes.get(i), 0l));
                }
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
                notesToSend = getCurrentNotes();
            }
        }
        //nothing to send otherwise
        return notesToSend;
    }

    private Pair<String, Long> delayedNote(String note, Long delay) {
        return new Pair<>(note, delay);
    }

    /**
     * Returns the list of notes to play based on the cursor position and the play mode.
     * It automatically increments the cursor as well!
     * @return
     */
    private ArrayList<Pair<String, Long>> getCurrentNotes() {
        ArrayList<Pair<String, Long>> notesToReturn = new ArrayList<>();
        if (playMode == PlayModeTypes.SCALE) {
            //SCALE
            // add curent note
            notesToReturn.add(delayedNote(notes.get(cursor), 0l));
            cursor = (cursor+1) % notes.size();

        } else if (playMode == PlayModeTypes.ARPEGGIO) {
            //ARPEGGIO
            // add all notes up to current with delay
            for (int i = 0; i < cursor+1; i++) {
                notesToReturn.add(delayedNote(notes.get(i), i * rules.getNotesDelayMs()));
            }
            cursor = (cursor+1) % notes.size();

        } else if (playMode == PlayModeTypes.REPEAT) {
            //REPEAT
            //add all notes "cursor" times with delay
            for (int i = 0; i < cursor+1; i++) {
                for (int j = 0; j < notes.size(); j++) {
                    notesToReturn.add(delayedNote(notes.get(j), i * rules.getNotesDelayMs()));
                }
            }
            cursor = (cursor+1) % repeatModeMax;

        }
        return notesToReturn;
    }

    /**
     * Resets the command to its original state (resets the cursor position).
     */
    public void reset() {
        cursor = 0;
    }
}
