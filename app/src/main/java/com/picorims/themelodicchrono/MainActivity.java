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

package com.picorims.themelodicchrono;

import static com.picorims.themelodicchrono.models.Units.UNITS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.picorims.themelodicchrono.models.Rules;
import com.picorims.themelodicchrono.sound.SoundGenerator;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    public static final double NOTE_DURATION = 0.5;

    //components
    private Button startChronoBtn;
    private Button stopChronoBtn;
    private Button resetChronoBtn;
    private TextView chronoDisplayText;

    private EditText textRules;
    private Button applyRules;
    private TextView errorMsg;

    private EditText delayInput;

    // chrono
    public static final long CHRONO_MS_FREQUENCY = 1000/60; //0.1s
    private Handler handler;
    private Boolean chronoStopped = true;
    private Runnable chronoTask;
    private long unixStart = -1; //-1 when inactive
    private long unixThen = -1;
    private long unixAtStop = -1;

    // rules
    // I am not motivated to learn Fragments + ViewModel + LiveData + FragmentManager + etc.
    // Just for a very basic app for personal use, so I have chosen to define rules through
    // a command line system.
    private String commandSyntax = "" +
            "how to read:\n" +
            "\n" +
            "- <> is mandatory\n" +
            "- [...='v'] is optional\n" +
            "  and takes v by default\n" +
            "- A | B means A or B\n" +
            "\n" +
            "\n" +
            "command syntax (on one line!):\n" +
            "\n" +
            "<play_mode>\n" +
            "   <timestamp>\n" +
            "   play <note_list>\n" +
            "   [note_play_mode='repeat']\n" +
            "\n" +
            "where:\n" +
            "- <play_mode> =\n" +
            "   - 'at':\n" +
            "       * at specified moment\n" +
            "   - 'every':\n" +
            "       * at every given frequency\n" +
            "\n" +
            "- <timestamp> = [?h]:[?m]:[?s]:[?ds]\n" +
            "   * (ex: '5s'; '1m'; '1h:30m'; etc.)\n" +
            "\n" +
            "- <note_list> = <note>[,<note>,...]\n" +
            "   - <note> =\n" +
            "       * note letter (ABCDEFG)\n" +
            "         with eventually # or b\n" +
            "   * (ex: 'C,Eb,G')\n" +
            "\n" +
            "- <note_play_mode> =\n" +
            "   - 'scale':\n" +
            "       * plays 1, 2, 3, 1...\n" +
            "   - 'arpeggio':\n" +
            "       * plays 1, 1+2, 1+2+3, 1...\n" +
            "   - 'repeat' [max_repeats=1]:\n" +
            "       * plays all 1, 2, n times,\n" +
            "         up to [max_repeats], then back to 1\n" +
            "   - [max_repeats]:\n" +
            "       * integer\n" +
            "   * NOTE: it has no effect on AT command!\n" +
            "\n" +
            "\n" +
            "examples:\n" +
            "\n" +
            "- every 5s play C,E,G arpeggio\n" +
            "   * (C then CE then CEG then C...)\n" +
            "- every 1m play C repeat 3\n" +
            "   * (C then CC then CCC then C...)\n" +
            "- every 1h play C,E,G repeat 2\n" +
            "   * (CEG then CEGCEG then CEG...)\n" +
            "- at 1m:30s play C\n" +
            "   * (plays C once)\n";
    private String rulesCommands = "";
    private Rules rules = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load layout
        setContentView(R.layout.activity_main);

        // get components
        startChronoBtn = findViewById(R.id.start_chrono);
        stopChronoBtn = findViewById(R.id.stop_chrono);
        resetChronoBtn = findViewById(R.id.reset_chrono);
        chronoDisplayText = findViewById(R.id.chrono_display);

        TextView cmdSyntax = findViewById(R.id.command_syntax);
        textRules = findViewById(R.id.rules_text);
        applyRules = findViewById(R.id.apply_rules);
        errorMsg = findViewById(R.id.error_message);
        delayInput = findViewById(R.id.delay_input);

        // write rules
        cmdSyntax.setText(commandSyntax);

        // init delay
        delayInput.setText("250");

        // init chrono
        resetChrono();

        // define chrono loop
        Activity thisActivity = this;
        chronoTask = new Runnable() {
            @Override
            public void run() {
                if (!chronoStopped) {
                    // initialization
                    long unixNow = System.currentTimeMillis();

                    // we start from 0
                    if (unixThen == -1) {
                        unixThen = unixNow; // reference for last loop call
                    }
                    if (unixStart == -1) {
                        unixStart = unixNow; // reference for time elapsed
                    }
                    // we were paused and have to restart from where we were
                    // to do this, we shift the start by the time ellapsed during
                    // the pause.
                    if (unixAtStop != -1) {
                        long ellapsedWhilePaused = unixNow - unixAtStop;
                        unixStart += ellapsedWhilePaused;
                        unixAtStop = -1; //clear last stop to not get here again
                    }

                    //durations
                    long ellapsed = unixNow - unixStart;
                    long ellapsedThen = unixThen - unixStart;
                    long loopDuration = unixNow - unixThen;

                    //display
                    long diffHours = ellapsed / UNITS.get("h");
                    long diffMinutes = (ellapsed / UNITS.get("m")) % 60;
                    long diffSeconds = (ellapsed / UNITS.get("s")) % 60;
                    long diffDeciseconds = (ellapsed / UNITS.get("ds")) % 10;

                    String time = strWithAZero(diffHours)
                            + ":" + strWithAZero(diffMinutes)
                            + ":" + strWithAZero(diffSeconds)
                            + "." + diffDeciseconds;
                    chronoDisplayText.setText(String.valueOf(time));

                    //play notes
                    if (rules != null) {
                        ArrayList<Pair<String, Long>> notesToPlay = rules.getNotesToPlay(ellapsedThen, ellapsed);
                        for (int i = 0; i < notesToPlay.size(); i++) {
                            Pair<String, Long> note = notesToPlay.get(i);
                            String noteStr = note.first;
                            long delay = note.second;
                            SoundGenerator.playNote(noteStr, NOTE_DURATION, delay, thisActivity);
                        }
                        if (notesToPlay.size() > 0) Log.d(TAG, "run: " + notesToPlay);
                    }

                    // end of loop updates
                    unixThen = unixNow;

                    //loop by sending a delayed call to this runnable
                    MainActivity.this.handler.postDelayed(this, CHRONO_MS_FREQUENCY);
                }
            }
        };


        // define event handlers

        // chrono control
        startChronoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startChrono();
            }
        });

        stopChronoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopChrono();
            }
        });

        resetChronoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetChrono();
                if (rules != null) rules.reset();
            }
        });

        applyRules.setOnClickListener((View v) -> {
            String commands = textRules.getText().toString();
            if (isEmpty(textRules)) {
                //clear
                rules = null;
                showToast("Cleared rules.");
            } else {
                //load
                long notesDelayMs = Long.parseLong(delayInput.getText().toString());
                Rules newRules = new Rules(commands, notesDelayMs);
                if (newRules.isSuccessfullyParsed()) {
                    //success
                    rules = newRules;
                    errorMsg.setText("");
                    showToast("Loaded rules.");
                } else {
                    //error
                    String error = newRules.getErrorMessage();
                    errorMsg.setText(error);
                    showToast("Couldn't load rules.");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopChrono();
    }

    /**
     * Launches a handler to run the chrono and update it every 0.1s
     */
    private void startChrono() {
        if (chronoStopped) {
            handler = new Handler();
            chronoStopped = false;
            handler.postDelayed(chronoTask, CHRONO_MS_FREQUENCY);
        }
    }

    /**
     * Terminates the chrono looper
     */
    private void stopChrono() {
        if (!chronoStopped) {
            chronoStopped = true;
            unixAtStop = System.currentTimeMillis();
        }
    }

    /**
     * Puts back the chrono in an initialized state, erasing tracked time
     */
    private void resetChrono() {
        unixStart = -1;
        unixThen = -1;
        unixAtStop = -1;
        chronoDisplayText.setText("00:00:00.0");
    }

    /**
     * Returns a String of the value with a leading zero if it is below 10.
     * @param v The value to stringify
     * @return The created string
     */
    private String strWithAZero(long v) {
        return (v < 10)? "0" + v : String.valueOf(v);
    }

    /**
     * Show a toast with the given message on the screen.
     * @param msg
     */
    private void showToast(String msg) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, msg, duration);
        toast.show();
    }

    /**
     * Check if an EditText is empty.
     * See https://stackoverflow.com/questions/6290531/how-do-i-check-if-my-edittext-fields-are-empty
     * @param etText
     * @return
     */
    private boolean isEmpty(EditText etText) {
        return etText.getText().toString().trim().length() == 0;
    }
}