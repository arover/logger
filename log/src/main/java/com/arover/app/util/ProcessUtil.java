package com.arover.app.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * @author arover
 * created at 2022/3/10 00:07
 */

public class ProcessUtil {
   public @Nullable static String getProcessName(Context ctx) {
      ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
      List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
      if (runningAppProcesses == null) {
         return null;
      }

      for (ActivityManager.RunningAppProcessInfo info : runningAppProcesses) {
         if (info.pid == Process.myPid()) {
            return info.processName;
         }
      }

      return null;
   }

   public static boolean isMainProcess(Context ctx){
      String processName = getProcessName(ctx);
      return ctx.getPackageName().equals(processName) || processName == null;
   }

   public static boolean isMainProcess(Context ctx, String processName){
      return ctx.getPackageName().equals(processName) || processName == null;
   }
   /**
    *  get log folder name by process name,
    * @param context
    * @param prefix optional, nullable, default is "app_" , main process log will
    *               saved in "app_main", process "xxx" 's log will saved in
    *               "app_xxx" etc.
    *
    */
   public static String getProcessNameWithPrefix(Context context,
                                                 @Nullable String processName,
                                                 @Nullable String prefix) {

      boolean isMainProcess = context.getPackageName().equals(processName) || processName == null;
      StringBuilder defaultPrefix = new StringBuilder();
      if(prefix != null){
         defaultPrefix.append(prefix);
      } else {
         defaultPrefix.append("app_");
      }
      StringBuilder folderName = null;
      if(isMainProcess){
         folderName = defaultPrefix.append("main");
      } else {
         if(processName.contains(":")){
            String[] names = processName.split(":");
            if(names.length >= 2){
               folderName = defaultPrefix.append(names[1]);
            }
         }
         if(folderName ==  null){
            folderName = defaultPrefix.append(Process.myPid());
         }
      }
      return folderName.toString();
   }
}
