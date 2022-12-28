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

import static com.picorims.themelodicchrono.models.Units.UNITS;

import java.util.Arrays;
import java.util.List;

/**
 * Class that parses a set of commands (the "rules") and determines when shall the different
 * notes play.
 */
public class Rules {
    private boolean successfullyParsed;
    private String errorMessage = null;

    public Rules(String commands) {
        try {
            loadRules(commands);
            successfullyParsed = true;
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
            successfullyParsed = false;
        }

    }

    /**
     * Is true if the rules were loaded successfully.
     * If not, the error can be obtained using getErrorMessage().
     * @return
     */
    public boolean isSuccessfullyParsed() {
        return successfullyParsed;
    }

    /**
     * The error message sent by loadRules(). null if no message was provided.
     * @return
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Try to load rules by parsing the command and loading its data. If a syntax error is met,
     * an exception is thrown.
     * @throws IllegalArgumentException
     * @param rules the list of commands separated by line breaks.
     */
    private void loadRules(String rules) {
        //get commands
        String validateCommandRegex = "^(at|every) ([0-9]{1,2}([hms]|ds)[:]?){0,4} play ([A-G][#b]?[,]?)+( (scale|arpeggio|repeat( [0-9]+)?))?$";
        String validateTimestamp = "^.*[^:]$";
        String validateNoteList = "^.*[^,]$";
        List<String> commands = Arrays.asList(rules.split("\n"));

        //TODO build data storage object (classes?) to have something queryable in the chrono loop

        for (int i = 0; i < commands.size(); i++) {
            String cmd = commands.get(i);
            if (!cmd.matches(validateCommandRegex)) throw new IllegalArgumentException("Invalid command syntax for command " + i + ":\n" + cmd);
            //get arguments of command
            List<String> args = Arrays.asList(cmd.split(" "));

            //parse command
            String playMode = args.get(0);

            //get timestamp as unix
            String timestamp = args.get(1);
            if (!timestamp.matches(validateTimestamp)) throw new IllegalArgumentException("Invalid timestamp at line " + i + ":" + timestamp);
            long msValue = timestampToUnix(timestamp);

            //get note list
            String noteList = args.get(3);
            if (!noteList.matches(validateNoteList)) throw new IllegalArgumentException("Invalid note list at line " + i + ":" + noteList);
            // TODO split note list

            //get repeat rule if exist
            String notePlayMode = (args.size() > 5)? args.get(4) : "";
            String notePlayModeMax = (args.size() > 6)? args.get(5) : "";

            //TODO do stuff with it
        }
    }

    /**
     * Converts a string timestamp into its value in milliseconds (for use with unix timestamps).
     * @param timestamp
     * @return
     */
    private long timestampToUnix(String timestamp) {
        List<String> durations = Arrays.asList(timestamp.split(":"));
        long unix = 0;

        for (String duration : durations) {
            boolean found = false;
            //verify if it matches one of the available unit
            for (String unit : UNITS.keySet()) {
                if (matchesUnit(duration, unit)) {
                    //if so, add its milliseconds equivalent to the timestamp.
                    String valueStr = duration.replace(unit,"");
                    int value = Integer.parseInt(valueStr);
                    unix += value * UNITS.get(unit);
                    found = true;
                    break;
                }
            }
            //if not, it isn't valid
            if (!found) throw new IllegalArgumentException(duration + " uses an invalid unit " +
                    "in timestamp: " + timestamp);
        }

        return unix;
    }

    /**
     * Returns if a string matches a pattern of a number plus a unit (5h, 15s, etc.)
     * @param str String to verify
     * @param unit unit to assert
     * @return the validity of the string
     */
    private boolean matchesUnit(String str, String unit) {
        String unitRegex = "^.*{0}$";
        return str.matches(unitRegex.replace("{0}", unit));
    }
}
