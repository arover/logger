package util;

import java.io.Closeable;

/**
 * @author MZY
 * created at 2021/3/16 11:52
 */

public class IoUtil {
    public static void closeQuietly(Closeable closeable) {
        if(closeable != null){
            try{
                closeable.close();
            }catch(Exception ignored){}
        }
    }
}
