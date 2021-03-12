package com.arover.logger;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.arover.app.logger.Log;
import com.arover.app.logger.LogWriterThread;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"onCreate");
        findViewById(R.id.button).setOnClickListener(v -> {
            decryptLog();
        });

        findViewById(R.id.log).setOnClickListener(v -> {
            Log.d(TAG,"on press log"+System.currentTimeMillis());
        });
    }

    private void decryptLog() {
        LogWriterThread.instance.decryptLog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }
}