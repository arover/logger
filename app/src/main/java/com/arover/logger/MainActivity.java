package com.arover.logger;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arover.app.Util;
import com.arover.app.crypto.RsaCipher;
import com.arover.app.logger.Log;

import java.io.File;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PICKFILE_RESULT_CODE = 101;
    private EditText textInput;
    private KeyPair keypair;
    private byte[] encrypteInputText;
    private TextView resultTxt;
    private TextView resultTxt2;

    @SuppressWarnings("handlerleak")
    Handler handler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "test log interval = " + System.currentTimeMillis());
            handler.sendEmptyMessageDelayed(0, 1000);
        }
    };

    private Uri logFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_main);
        textInput = (EditText) findViewById(R.id.text);
        resultTxt = (TextView) findViewById(R.id.result);
        resultTxt2 = (TextView) findViewById(R.id.result2);

        findViewById(R.id.decrypt_file).setOnClickListener(v -> {
            if (logFileUri == null) {
                Toast.makeText(getApplicationContext(), "please choose log file first!", Toast.LENGTH_SHORT).show();
            } else {
                decryptLog(logFileUri);
            }
        });
        findViewById(R.id.choose_file_btn).setOnClickListener(v -> {
            chooseFile();
        });
//        findViewById(R.id.decrypt_btn).setOnClickListener(v -> {
//            decryptTxt();
//        });
//
//        findViewById(R.id.encrypt_btn).setOnClickListener(v -> {
//            encryptTxt();
//        });

        findViewById(R.id.log).setOnClickListener(v -> {
            Log.d(TAG, "on press log" + System.currentTimeMillis());
        });
        findViewById(R.id.gen_key_btn).setOnClickListener(v -> {
            createKeyPair();
        });
        findViewById(R.id.flush_btn).setOnClickListener(v -> {
            Log.flush();
        });

        //log test print log per second.
//        handler.sendEmptyMessageDelayed(0,1000);
    }

    private void createKeyPair() {
        try {
            keypair = RsaCipher.createKeyPair();
            resultTxt.setText(getString(R.string.public_key_is, Util.bytesToHexString(keypair.getPublic().getEncoded())));
            resultTxt2.setText(getString(R.string.private_key_is, Util.bytesToHexString(keypair.getPrivate().getEncoded())));
            android.util.Log.d(TAG, "public key=" + resultTxt.getText());
            android.util.Log.d(TAG, "private key=" + resultTxt2.getText());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void encryptTxt() {
        String input = textInput.getText().toString();
        android.util.Log.d(TAG, "encryptTxt=" + input);
        try {
            encrypteInputText = RsaCipher.encrypt(input.getBytes(), keypair.getPublic().getEncoded());
            resultTxt.setText(Util.bytesToHexString(encrypteInputText));
            android.util.Log.d(TAG, "encryptTxt len=" + encrypteInputText.length);
        } catch (Exception e) {
            android.util.Log.d(TAG, "encryptTxt=" + input, e);
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void decryptTxt() {

        try {
            byte[] decryptBytes = RsaCipher.decrypt(encrypteInputText, keypair.getPrivate().getEncoded());
            resultTxt.setText(new String(decryptBytes));
        } catch (Exception e) {
            android.util.Log.e(TAG, "decryptTxt=", e);
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void decryptLog(Uri fileUri) {
        Context appContext = getApplicationContext();
        String file = fileUri.getPath();

        if(file != null && !file.endsWith(".log")){
            Toast.makeText(getApplicationContext(), "invalid file, choose .log file!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            android.util.Log.d(TAG, "decryptLog " + file);
            byte[] privateKey = Util.hexStringToBytes(((App) getApplication()).getPrivateKey());
            try {
                Log.decryptLogFile(appContext, fileUri, privateKey);
                runOnUiThread(() -> Toast.makeText(appContext, "decrypt log file success", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(appContext, "decrypt log file error:" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.flush();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        handler = null;
        Log.flush();
        super.onDestroy();
    }


    public void chooseFile() {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("application/*");
        startActivityForResult(
                Intent.createChooser(chooseFile, getString(R.string.choose_log_file_title)),
                PICKFILE_RESULT_CODE
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        logFileUri = data.getData();
        decryptLog(logFileUri);
    }
}