package com.arover.app;

import android.app.ActivityManager;
import android.content.Context;

import java.io.Closeable;
import java.util.List;

/**
 * @author MZY
 * created at 2021/3/13 17:24
 */

public class Util {

    public static String getCurrentProcessName(Context ctx, int pid){
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        if(runningAppProcesses == null){
            return null;
        }

        for(ActivityManager.RunningAppProcessInfo info : runningAppProcesses){
            if(info.pid == pid){
                return info.processName;
            }
        }

        return null;
    }

    public static String bytesToHexString(byte[] data) {
        if (data == null) return "";
        StringBuilder s = new StringBuilder(data.length * 2);
        for (byte b : data) {
            s.append(String.format("%02x", b & 0xff));
        }
        return s.toString();
    }

    public static byte[] hexStringToBytes(String s) {
        if (s.length() == 0) {
            return new byte[0];
        }

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

    public static void closeQuietly(Closeable closeable) {
        if(closeable != null){
            try{
                closeable.close();
            }catch(Exception ignored){}
        }
    }
}
