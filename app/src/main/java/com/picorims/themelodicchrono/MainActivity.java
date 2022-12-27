package com.picorims.themelodicchrono;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "App::MainActivity";
    //components
    private Button startChronoBtn;
    private Button stopChronoBtn;
    private Button resetChronoBtn;
    private TextView chronoDisplayText;

    private Button addRuleBtn;
    private LinearLayout rulesContainer;

    // chrono
    public static final long CHRONO_MS_FREQUENCY = 100; //0.1s
    private Handler handler;
    private Boolean chronoStopped = true;
    private Runnable chronoTask;
    private long unixStart = -1; //-1 when inactive
    private long unixThen = -1;
    private long unixAtStop = -1;

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

        addRuleBtn = findViewById(R.id.add_rule_button);
        rulesContainer = findViewById(R.id.rules_container);

        // init chrono
        resetChrono();

        // define chrono loop
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
                    long loopDuration = unixNow - unixThen;

                    //display
                    long diffHours = ellapsed / 3600000;// /1000 /60 /60
                    long diffMinutes = (ellapsed / 60000) % 60;
                    long diffSeconds = (ellapsed / 1000) % 60;
                    long diffDeciseconds = (ellapsed / 100) % 10;

                    String time = strWithAZero(diffHours)
                            + ":" + strWithAZero(diffMinutes)
                            + ":" + strWithAZero(diffSeconds)
                            + "." + diffDeciseconds;
                    chronoDisplayText.setText(String.valueOf(time));

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
            }
        });

        //rules management
        addRuleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addRule();
            }
        });
    }

    /**
     * Launches a handler to run the chrono and update it every 0.1s
     */
    private void startChrono() {
        handler = new Handler();
        chronoStopped = false;
        handler.postDelayed(chronoTask, CHRONO_MS_FREQUENCY);
    }

    /**
     * Terminates the chrono looper
     */
    private void stopChrono() {
        chronoStopped = true;
        unixAtStop = System.currentTimeMillis();
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
     * Create a new UI component instance for managing a new rule
     */
    private void addRule() {
        LayoutInflater inflater = getLayoutInflater();
        View childLayout = inflater.inflate(R.layout.component_rule, rulesContainer, false);
        rulesContainer.addView(childLayout);
    }
}