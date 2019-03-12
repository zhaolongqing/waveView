package zlq.com.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



    }

    @Override
    protected void onResume() {
        super.onResume();
        final ECGScrollView ecgScrollView = findViewById(R.id.ecg_view);

        final ArrayQueue arrayQueue = new ArrayQueue(2000);
        ecgScrollView.setData(arrayQueue);
        for (int i = 0; i < 2000; i++) {
            arrayQueue.pop(i + 1);
            ecgScrollView.invalidate();
        }
    }
}
