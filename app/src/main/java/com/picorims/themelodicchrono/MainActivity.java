package com.picorims.themelodicchrono;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    //components
    private Button startChronoBtn;
    private Button stopChronoBtn;
    private Button resetChronoBtn;
    private TextView chronoDisplayText;

    // chrono
    public static final long CHRONO_MS_FREQUENCY = 100; //0.1s
    private Handler handler;
    private Boolean chronoStopped = true;
    private Runnable chronoTask;
    private int counter = 0;

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

        // init components
        chronoDisplayText.setText("0");

        // define chrono loop
        chronoTask = new Runnable() {
            @Override
            public void run() {
                if (!chronoStopped) {
                    counter++;
                    chronoDisplayText.setText(String.valueOf(counter));

                    //loop by sending a delayed call to this runnable
                    MainActivity.this.handler.postDelayed(this, CHRONO_MS_FREQUENCY);
                }
            }
        };


        // define event handlers
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
    }
}