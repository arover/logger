package com.arover.logger.cmdtool;

import static util.DataUtil.bytesToHexString;
import static util.IoUtil.closeQuietly;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Security;
import java.util.Arrays;

import crypto.AesCbcCipher;
import crypto.RsaCipher;


public class LogCmdTool {

    public static final int ENCRYPT_IV_LEN = 256;
    public static final int ENCRYPT_KEY_LEN = 256;
    static final byte MODE_ENCRYPT_LOG = 1;
    static final byte MODE_PLAIN_LOG = 0;

    public static void main(String[] args) throws Exception {

        System.out.println("main() called with: args = " + Arrays.toString(args));
        if (args.length < 2) {
            System.out.print("Usage: java -jar cmdtool-all.jar your_log_private.key s_xxx_xxx.log");
            return;
        }
        String key = args[0];
        String filename = args[1];

        byte[] keyContent;
        try {
            keyContent = readKey(key);
        }catch (IOException e){
            System.out.println("key file: "+key+" is not found.");
            return;
        }
        decryptLogFile(filename, keyContent);
    }

    private static byte[] readKey(String key) throws IOException {

        try(FileInputStream in = new FileInputStream(key)) {
            byte[] buf = new byte[2048];
            int len = in.read(buf);
            if (len <= 1024) {
                throw new IllegalStateException("empty private key file?");
            }
            byte[] result = new byte[len];
            System.arraycopy(buf, 0, result, 0, len);
            return result;
        } catch (IOException e) {
            throw e;
        }
    }


    public static void decryptLogFile(String encryptedFilename, byte[] privateKey) throws Exception {
        DataInputStream in = null;
        FileOutputStream out = null;
        BouncyCastleProvider p = new BouncyCastleProvider();
        Security.addProvider(p);

        System.out.println("read file = " + encryptedFilename);
        try {
            File encryptedLogFile = new File(encryptedFilename);
            if (!encryptedLogFile.exists()) {
                System.out.println("ERROR: " + encryptedFilename + " not exists.");
                return;
            }
            String outFileName = encryptedFilename + "_decrypted.log";
            in = new DataInputStream(new FileInputStream(encryptedLogFile));
            out = new FileOutputStream(outFileName);
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
                        System.err.println("readInt = " + e.getMessage());
                        throw new IllegalStateException("read log length data error.", e);
                    } else {
                        System.out.println("parse log end.");
                    }
                    break;
                }
                System.out.println("size = " + len);
//                if (len > BUFFER_SIZE) {
//                    System.out.print("invalid log length, are you sure log file is right??? = " + len);
//                    return;
//                }
                byte mode = in.readByte();
                System.out.println("mode = " + mode);
                if (mode == MODE_ENCRYPT_LOG) {
                    n = in.read(iv);
//                    System.out.println("iv = " + bytesToHexString(iv));
                    if (n != iv.length) {
                        throw new IllegalStateException("read iv data error n != iv.length.");
                    }
                    n = in.read(key);
//                    System.out.println("key = " + bytesToHexString(key));
                    if (n != key.length) {
                        throw new IllegalStateException("read key data error n != key.length.");
                    }
                }
                buf = new byte[len];
                n = in.read(buf);
                if (n != len) {
                    System.out.println("encrypted log length is not correct, read len=" + n + ",len=" + len);
                    throw new IllegalStateException("read log data error, read log length is not equals len.");
                }
                if (mode == MODE_ENCRYPT_LOG) {
                    byte[] decryptLog;
                    try {
                        byte[] decryptKey = RsaCipher.decrypt(p.getName(), key, privateKey);
                        byte[] decryptIv = RsaCipher.decrypt(p.getName(), iv, privateKey);

//                        System.out.println("decryptKey" + bytesToHexString(decryptKey));
//                        System.out.println("decryptKey" + bytesToHexString(decryptIv));

                        decryptLog = AesCbcCipher.decrypt(buf, decryptKey, decryptIv);

//                        System.out.println("write decryptLog = " + bytesToHexString(decryptLog));

                        out.write(decryptLog);
                    } catch (Exception e) {
                        System.out.println("decrypt log error" + e);
                        System.out.println("decryptLog failed = " + bytesToHexString(buf));
                        throw e;
                    }
                } else {
                    System.out.println("write plain = " + bytesToHexString(buf));
                    out.write(buf);
                }
            }
            out.flush();
            System.out.println("================OK==================see: "+outFileName);
        } catch (Exception e) {
            System.err.println("parse encrypted log error:" + e);
            throw e;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }

    }

}