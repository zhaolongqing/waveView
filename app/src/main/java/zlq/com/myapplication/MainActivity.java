package zlq.com.myapplication;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

public class MainActivity extends AppCompatActivity {

    static int i = 0;

    private static final String TAG = "MainActivity";
    final Handler handler = new Handler();
    final ArrayQueue arrayQueue = new ArrayQueue(2000);
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


    }

    @Override
    protected void onResume() {
        super.onResume();
        ecgScrollView = findViewById(R.id.ecg_view);

        ecgScrollView.setData(arrayQueue);


        ecgScrollView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                handler.postDelayed(runnable, 20);
                handler.post(runnable);
            }
        });
    }
}
