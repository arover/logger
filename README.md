# Android 多功能日志库

## Features: 
- RSA & AES encryption
- daily roation & auto ratation by file size
- auto remove old logs
- customize log folder
- multi process logging
- pure java implementation NO JNI, smallest lib size.
  
## Usage
=======
## 功能 & 特色
- 初始化时之前即可打印日志，日志将保存至内存，文件日志配置后再自动输出至文件。
- 日志加密 （RSA 和 AES 两种加密方式结合后加密日志，兼顾安全与高效）
- 日志自动压缩
- 旧日志定期删除
- 自定义日志文件夹
- 支持多进程
- 无JNI，纯java。

## 用法

```groovy
//step1
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
// step2
dependencies {
    implementation 'com.github.arover:logger:2.1.0'
}

```

```java

import com.arover.app.logger.Log;

public class App extends Application {
    static final String TAG = "App";
    @Override
    public void onCreate() {
        super.onCreate();
        Alog.d(TAG,"onCreate");

        String publicKey = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100cf9d8c3a47e7e6268e12d87f4eb09ff503fbb41dfa78e1e473e636967e3998dbb6e74e363f7a241d5b994359c3c134b2f1e4f9e6af197137e921b3870f9c0d798790d00f0b7e1eab6aa0965b971dca362de9b0d38d53cf78b6203a28210e00521c143fe230c387edb5a9868e58b60a871906793bc5cc288dbeb740963d844121e571622080ba6c40df1f9e22a8ccd76837e3e74d8f4c6a693b8200db29227268503071bc976f979bbec2c8666ef535d4b4a5eb479fc139e24c7046c75bff1a73eacf8d0c9136ad0afff73911f7049c1aa4b8af1867c1a2a45707e6f2c35e36ff955ee50d500e415854432e4960ca88a70111de72eb96848e7e452a07c45f17f50203010001";

        new LoggerManager.Builder(this)
                .enableLogcat(BuildConfig.DEBUG) //是否开启logcat输出
                .level(LoggerManager.Level.DEBUG) //日志级别
                .encryptWithPublicKey(publicKey) //可选参数，是否加密，密钥为空或者null则不开启加密。生成密钥请构建app然后生成。
//                .rootFolder("Log") //可选参数，默认日志文件夹名为logs。
//                .processLogFolder(folderName) //可选参数，自定义各个进程的日志文件夹名。
                .build()
                .deleteOldLogsDelayed(7);
        // use it like android.util.Log.
        Log.d(TAG,"app is launching...");
    }
}
```
## 解密
```shell
>java -jar decrypt.jar
```

## 日志解密：
1. 下载本项目根目录的cmdtool-all.jar
2. 请用1.8以上版本java执行解密；

Usage: java -jar cmdtool-all.jar your_log_private.key s_xxx_xxx.log


示例：
```shell
java -jar cmdtool-all.jar your_log_private.key s_xxx_xxx.log
```

