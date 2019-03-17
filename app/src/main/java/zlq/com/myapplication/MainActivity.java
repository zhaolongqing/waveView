package zlq.com.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    Random random = new Random();
    private WaveSurfaceView waveSurfaceView;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            waveSurfaceView.drawLine((int) (random.nextFloat() * 2000 - random.nextFloat() * 2000));
            handler.postDelayed(runnable, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onResume() {
        super.onResume();
        waveSurfaceView = findViewById(R.id.ecg_view);
        waveSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.post(runnable);
            }
        });
    }
}
