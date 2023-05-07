package com.arover.logger.testapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

import com.arover.app.crypto.RsaCipher;
import com.arover.app.logger.Alog;
import com.arover.app.util.DataUtil;
import com.arover.app.util.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedList;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PICK_FILE_RESULT_CODE = 101;

    private KeyPair keypair;
    private byte[] encryptedInputText;
    private TextView resultTxt;
    private TextView resultTxt2;
    private Uri logFileUri;
    private byte[] privateKey;

    private final LinkedList<Disposable> disposables = new LinkedList<>();

    @SuppressWarnings("handlerleak")
    Handler handler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Alog.d(TAG, "test log interval = " + System.currentTimeMillis());
            handler.sendEmptyMessageDelayed(0, 1000);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Alog.d(TAG, "onCreate");

        setContentView(R.layout.activity_main);

        resultTxt = findViewById(R.id.result);
        resultTxt2 = findViewById(R.id.result2);

        findViewById(R.id.decrypt_file).setOnClickListener(v -> {
            if (logFileUri == null) {
                Toast.makeText(getApplicationContext(), "please choose log file first!",
                        Toast.LENGTH_SHORT).show();
            } else {
                decryptLog(logFileUri);
            }
        });
        findViewById(R.id.choose_file_btn).setOnClickListener(v -> {
            chooseFile();
        });
        findViewById(R.id.load_private_key).setOnClickListener(v -> {
            loadPrivateKey();
        });
//        findViewById(R.id.decrypt_btn).setOnClickListener(v -> {
//            decryptTxt();
//        });
//
//        findViewById(R.id.encrypt_btn).setOnClickListener(v -> {
//            encryptTxt();
//        });

        findViewById(R.id.log).setOnClickListener(v -> {
            Alog.d(TAG, "on press log  " + System.currentTimeMillis()+", print some logs in files ... Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.");
        });
        findViewById(R.id.gen_key_btn).setOnClickListener(v -> {
            createKeyPair();
        });
        findViewById(R.id.flush_btn).setOnClickListener(v -> {
            Alog.flush();
        });

        //log test print log per second.
//        handler.sendEmptyMessageDelayed(0,1000);
    }

    private void loadPrivateKey() {
        Context appContext = getApplicationContext();
        autoClean(
                loadPrivateFile(appContext)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((byte[] key) -> {
                            privateKey = key;
                            android.util.Log.i(TAG,
                                    "loadPrivateFile key=" + DataUtil.bytesToHexString(privateKey));
                            Toast.makeText(getApplicationContext(), "load private key success",
                                    Toast.LENGTH_SHORT).show();
                        }, e -> Toast.makeText(getApplicationContext(), e.getMessage(),
                                Toast.LENGTH_SHORT).show()));
    }

    private Single<byte[]> loadPrivateFile(Context applicationContext) {
        return Single.create(emitter -> {

            File privateKeyFile = new File(
                    applicationContext.getExternalFilesDir(null) + "/log_private.key");

            if (!privateKeyFile.exists()) {
                emitter.onError(new FileNotFoundException("private key file not exist"));
                return;
            }

            InputStream in = null;
            try {
                in = new FileInputStream(privateKeyFile);
                byte[] buf = new byte[2048];
                int len = in.read(buf);
                if (len <= 1024) {
                    throw new IllegalStateException("empty private key file?");
                }
                byte[] result = new byte[len];
                System.arraycopy(buf, 0, result, 0, len);
                emitter.onSuccess(result);
            } catch (Exception e) {
                emitter.onError(e);
            } finally {
                IoUtil.closeQuietly(in);
            }
        });
    }

    private void createKeyPair() {
        try {
            keypair = RsaCipher.createKeyPair();
            resultTxt.setText(getString(R.string.public_key_is,
                    DataUtil.bytesToHexString(keypair.getPublic().getEncoded())));
            resultTxt2.setText(getString(R.string.private_key_is,
                    DataUtil.bytesToHexString(keypair.getPrivate().getEncoded())));
            Context appContext = getApplicationContext();
            privateKey = keypair.getPrivate().getEncoded();
            autoClean(
                    saveKeypairToFile(appContext, keypair)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> {

                            }, e -> Toast.makeText(getApplicationContext(), e.getMessage(),
                                    Toast.LENGTH_SHORT).show()));


            android.util.Log.d(TAG, "public key=" + resultTxt.getText());
            android.util.Log.d(TAG, "private key=" + resultTxt2.getText());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void autoClean(Disposable subscribeWith) {
        synchronized (disposables) {
            cleanDisposed();
            disposables.add(subscribeWith);
        }
    }

    private void cleanDisposed() {
        Iterator<Disposable> it;
        for (it = disposables.iterator(); it.hasNext(); ) {
            if (it.next().isDisposed()) {
                it.remove();
            }
        }
    }


    private Completable saveKeypairToFile(Context applicationContext, KeyPair keypair) {
        return Completable.create(emitter -> {
            File privateKeyFile = new File(applicationContext.getExternalFilesDir(null) +
                    "/log_private.key");
            File publicKeyFile = new File(applicationContext.getExternalFilesDir(null)
                    + "/log_public_key_hex_str.txt");
            OutputStream publicFileOut = null, privateFileOut = null;
            try {
                publicFileOut = new FileOutputStream(publicKeyFile);
                publicFileOut.write(DataUtil.bytesToHexString(keypair.getPublic().getEncoded()).getBytes());
                publicFileOut.flush();

                privateFileOut = new FileOutputStream(privateKeyFile);
                privateFileOut.write(keypair.getPrivate().getEncoded());
                privateFileOut.flush();

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(applicationContext,
                            "key file saved ",
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                IoUtil.closeQuietly(publicFileOut);
                IoUtil.closeQuietly(privateFileOut);
            }
        });
    }

//    private void encryptTxt() {
//        String input = textInput.getText().toString();
//        android.util.Log.d(TAG, "encryptTxt=" + input);
//        try {
//            encryptedInputText = RsaCipher
//                    .encrypt(input.getBytes(), keypair.getPublic().getEncoded());
//            resultTxt.setText(DataUtil.bytesToHexString(encryptedInputText));
//            android.util.Log.d(TAG, "encryptTxt len=" + encryptedInputText.length);
//        } catch (Exception e) {
//            android.util.Log.d(TAG, "encryptTxt=" + input, e);
//            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }

//    private void decryptTxt() {
//
//        try {
//            byte[] decryptBytes = RsaCipher
//                    .decrypt(encryptedInputText, keypair.getPrivate().getEncoded());
//            resultTxt.setText(new String(decryptBytes));
//        } catch (Exception e) {
//            android.util.Log.e(TAG, "decryptTxt=", e);
//            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }

    private void decryptLog(Uri fileUri) {
        Context appContext = getApplicationContext();
        String file = fileUri.getPath();

        if (file != null && !file.endsWith(".log")) {
            Toast.makeText(getApplicationContext(), "invalid file, choose .log file!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            android.util.Log.d(TAG, "decryptLog " + file);

            try {
                Alog.decryptLogFile(appContext, fileUri, privateKey);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(appContext, "decrypt log file success", Toast.LENGTH_SHORT)
                            .show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(appContext, "decrypt log file error:" + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Alog.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Alog.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Alog.flush();
        synchronized (disposables) {
            cleanDisposed();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        handler = null;
        Alog.flush();

        synchronized (disposables) {
            for (Disposable it : disposables) {
                it.dispose();
            }
            disposables.clear();
        }
        super.onDestroy();
    }


    public void chooseFile() {
        if(privateKey == null){
            Toast.makeText(getApplicationContext(), "please load private key first",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("application/*");
        startActivityForResult(
                Intent.createChooser(chooseFile, getString(R.string.choose_log_file_title)),
                PICK_FILE_RESULT_CODE
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