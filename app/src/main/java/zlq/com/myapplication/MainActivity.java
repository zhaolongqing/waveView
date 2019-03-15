package zlq.com.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.zhaogege.ecgviewlibrary.ArrayQueue;
import com.zhaogege.ecgviewlibrary.ECGPathWaveView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    static int i = 0;

    private static final String TAG = "MainActivity";
    final Handler handler = new Handler();
    static int xj = 1;
    Random random = new Random();

    final ArrayQueue arrayQueue = new ArrayQueue(50);
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            arrayQueue.pop(random.nextDouble() * 1000);
            ecgScrollView.drawXLine();
            handler.postDelayed(runnable, 10);
        }
    };
    private ECGPathWaveView ecgScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ecgScrollView = findViewById(R.id.ecg_view);

        ecgScrollView.setXQueue(arrayQueue);

        ecgScrollView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(null);
                handler.postDelayed(runnable, 20);
               /* for (int i = 0; i < 50; i++) {
                    arrayQueue.pop(i * xj);
                }
                ecgScrollView.drawWaveArray();
                xj++;*/
            }
        });
    }
}
