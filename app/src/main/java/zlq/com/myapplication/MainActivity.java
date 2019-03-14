package zlq.com.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    static int i = 0;

    private static final String TAG = "MainActivity";
    final Handler handler = new Handler();
    static int xj = 1;

    final ArrayQueue arrayQueue = new ArrayQueue(50);
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            arrayQueue.pop(i++);
            ecgScrollView.drawWave();
            handler.postDelayed(runnable, 10);
        }
    };
    private ECGScrollView ecgScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("fjfjfj")
                        .setTitle("djfjfjfjfj")
                        .create()
                        .show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        ecgScrollView = findViewById(R.id.ecg_view);

        ecgScrollView.setData(arrayQueue);

        ecgScrollView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.postDelayed(runnable, 20);
                handler.post(runnable);
               /* for (int i = 0; i < 50; i++) {
                    arrayQueue.pop(i * xj);
                }
                ecgScrollView.drawWaveArray();
                xj++;*/
            }
        });
    }
}
