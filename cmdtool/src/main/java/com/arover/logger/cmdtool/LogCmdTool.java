package com.arover.logger.cmdtool;

import static util.DataUtil.bytesToHexString;
import static util.IoUtil.closeQuietly;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import crypto.AesCbcCipher;
import crypto.RsaCipher;


public class LogCmdTool {

    public static final int ENCRYPT_IV_LEN = 256;
    public static final int ENCRYPT_KEY_LEN = 256;
//    public static final int BUFFER_SIZE = 1024 * 128;
    static final byte MODE_ENCRYPT_LOG = 1;
    static final byte MODE_PLAIN_LOG = 0;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.print("Usage: java -jar arover_log_cmdtool.jar key.txt some.log");
            return;
        }
        String key = args[1];
        String filename = args[2];

        String keyStr;
        try {
            keyStr = readKey(key);
        }catch (IOException e){
            System.out.println("key file: "+key+" is not found.");
            return;
        }
        decryptLogFile(filename, keyStr.getBytes());
    }

    private static String readKey(String key) throws IOException {

        BufferedInputStream inputStream = null;

        try {
            inputStream = new BufferedInputStream(new FileInputStream(key));

            int len = inputStream.available();
            byte[] data = new byte[len];
            inputStream.read(data, 0, len);

            return new String(data);
        } catch (IOException e) {
            throw e;
        } finally {
            closeQuietly(inputStream);
        }
    }


    public static void decryptLogFile(String encryptedFilename, byte[] privateKey) throws Exception {
        DataInputStream in = null;
        FileOutputStream out = null;

        System.out.println("read file = " + encryptedFilename);
        try {
            File encryptedLogFile = new File(encryptedFilename);
            if (!encryptedLogFile.exists()) {
                System.out.print("ERROR: " + encryptedFilename + " not exists.");
                return;
            }

            in = new DataInputStream(new FileInputStream(encryptedLogFile));
            out = new FileOutputStream(new File(encryptedFilename + "_decrypted.log"));
            byte[] buf;
            byte[] iv = new byte[ENCRYPT_IV_LEN];
            byte[] key = new byte[ENCRYPT_KEY_LEN];
            int n;
            for (; ; ) {
                int len;
                try {
                    len = in.readInt();
                } catch (Exception e) {
                    if (!(e instanceof EOFException)) {
                        System.err.print("readInt = " + e.getMessage());
                        throw new IllegalStateException("read log length data error.", e);
                    } else {
                        System.out.print("parse log end.");
                    }
                    break;
                }
                System.out.print("size = " + len);
//                if (len > BUFFER_SIZE) {
//                    System.out.print("invalid log length, are you sure log file is right??? = " + len);
//                    return;
//                }
                byte mode = in.readByte();
                System.out.print("mode = " + mode);
                if (mode == MODE_ENCRYPT_LOG) {
                    n = in.read(iv);
                    System.out.print("iv = " + bytesToHexString(iv));
                    if (n != iv.length) {
                        throw new IllegalStateException("read iv data error n != iv.length.");
                    }
                    n = in.read(key);
                    System.out.print("iv = " + bytesToHexString(key));
                    if (n != key.length) {
                        throw new IllegalStateException("read key data error n != key.length.");
                    }
                }
                buf = new byte[len];
                n = in.read(buf);
                if (n != len) {
                    System.out.print("encrypted log length is not correct, read len=" + n + ",len=" + len);
                    throw new IllegalStateException("read log data error, read log length is not equals len.");
                }
                if (mode == MODE_ENCRYPT_LOG) {
                    byte[] decryptLog;
                    try {
                        byte[] decryptKey = RsaCipher.decrypt(key, privateKey);
                        byte[] decryptIv = RsaCipher.decrypt(iv, privateKey);

//                        android.util.Log.d(TAG, "decryptKey" + bytesToHexString(decryptKey));
//                        android.util.Log.d(TAG, "decryptKey" + bytesToHexString(decryptIv));

                        decryptLog = AesCbcCipher.decrypt(buf, decryptKey, decryptIv);

//                        android.util.Log.v(TAG, "write decryptLog = " + bytesToHexString(decryptLog));

                        out.write(decryptLog);
                    } catch (Exception e) {
                        System.out.print("decrypt log error" + e);
                        System.out.print("decryptLog failed = " + bytesToHexString(buf));
                        out.write(buf);
                    }
                } else {
//                    android.util.Log.v(TAG, "write plain = " + bytesToHexString(buf));
                    out.write(buf);
                }
            }
            out.flush();

        } catch (Exception e) {
            System.err.print("parse encrypted log error:" + e);
            throw e;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }

    }

}