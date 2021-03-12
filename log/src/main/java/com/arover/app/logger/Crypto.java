package com.arover.app.logger;

import android.annotation.SuppressLint;
import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author MZY
 * created at 2021/3/11 15:19
 */

public class Crypto {

    private static SecretKey generateKey() throws Exception {
        // 根据指定的 RNG 算法, 创建安全随机数生成器

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        // 设置 密钥key的字节数组 作为安全随机数生成器的种子
//        random.setSeed(key);

        KeyGenerator gen = KeyGenerator.getInstance("AES");
        // 初始化算法生成器
        gen.init(128, random);

        // 生成 AES密钥对象, 也可以直接创建密钥对象: return new SecretKeySpec(key, ALGORITHM);
        return gen.generateKey();
    }


    @SuppressLint("GetInstance")
    public static byte[] aesEncrypt(byte[] key, String src) {

        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = null;//"算法/模式/补码方式"
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return cipher.doFinal(src.getBytes("utf-8"));
        } catch (Exception e) {
            return src.getBytes();
        }

//        return new Base64().encodeToString(encrypted);//此处使用BASE64做转码功能，同时能起到2次加密的作用。

    }
}
