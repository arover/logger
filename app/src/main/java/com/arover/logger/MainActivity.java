package com.arover.logger;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arover.app.Util;
import com.arover.app.crypto.RsaCipher;
import com.arover.app.logger.Log;
import com.arover.app.logger.LogWriterThread;

import java.io.File;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private EditText textInput;
    private KeyPair keypair;
    private byte[] encrypteInputText;
    private TextView resultTxt;
    private TextView resultTxt2;
    private TextView flushBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"onCreate");

        setContentView(R.layout.activity_main);
        textInput = (EditText)findViewById(R.id.text);
        resultTxt = (TextView)findViewById(R.id.result);
        resultTxt2 = (TextView)findViewById(R.id.result2);
        flushBtn = (TextView)findViewById(R.id.flush_btn);
        findViewById(R.id.decrypt_file).setOnClickListener(v -> {
            decryptLog();
        });

        findViewById(R.id.decrypt_btn).setOnClickListener(v -> {
            decryptTxt();
        });

        findViewById(R.id.encrypt_btn).setOnClickListener(v -> {
            encryptTxt();
        });

        findViewById(R.id.log).setOnClickListener(v -> {
            Log.d(TAG,"on press log"+System.currentTimeMillis());
        });
        findViewById(R.id.gen_key_btn).setOnClickListener(v -> {
           createKeyPair();
        });
        findViewById(R.id.flush_btn).setOnClickListener(v -> {
           Log.flush();
        });

    }

    private void createKeyPair() {
        try {
            keypair = RsaCipher.createKeyPair();
            resultTxt.setText(Util.bytesToHexString(keypair.getPublic().getEncoded()));
            resultTxt2.setText(Util.bytesToHexString(keypair.getPrivate().getEncoded()));
            android.util.Log.d(TAG,"public key="+resultTxt.getText());
            android.util.Log.d(TAG,"private key="+resultTxt2.getText());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void encryptTxt() {
        String input = textInput.getText().toString();
        android.util.Log.d(TAG,"encryptTxt="+input);
        try {
            encrypteInputText = RsaCipher.encrypt(input.getBytes(), keypair.getPublic().getEncoded());
            resultTxt.setText(Util.bytesToHexString(encrypteInputText));
            android.util.Log.d(TAG,"encryptTxt len="+encrypteInputText.length);
        } catch (Exception e) {
            android.util.Log.d(TAG,"encryptTxt="+input, e);
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void decryptTxt() {

        try {
            byte[] decryptBytes = RsaCipher.decrypt(encrypteInputText, keypair.getPrivate().getEncoded());
            resultTxt.setText(new String(decryptBytes));
        } catch (Exception e) {
            android.util.Log.e(TAG,"decryptTxt=", e);
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void decryptLog() {
        new Thread(() -> {
            File file = Log.getCurrentLogFile();
            android.util.Log.d(TAG,"decryptLog "+file.getAbsolutePath());
            Log.decryptLogFile(file);
        }).start();
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
    protected void onStop() {
        super.onStop();
        Log.flush();
    }

    @Override
    protected void onDestroy() {
        Log.flush();
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }
}