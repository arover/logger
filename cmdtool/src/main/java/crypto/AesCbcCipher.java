package crypto;

import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author MZY
 * created at 2021/3/13 15:57
 */
public class AesCbcCipher {

    private static final String CIPHER = "AES/CBC/PKCS5PADDING";

    public static byte[] genRandomBytes(int len){
        Random random = new Random();
        byte[] data = new byte[len];
        for (int i = 0; i < data.length; i++)
            data[i] = (byte) random.nextInt(256);
        return data;
    }

    public static byte[] encrypt(byte[] plain, byte[] key, byte[] ivParameter) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(ivParameter);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
        return cipher.doFinal(plain);
    }

    public static byte[] decrypt(byte[] encrypted, byte[] key, byte[] ivParameter) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(ivParameter);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

        return cipher.doFinal(encrypted);
    }
}
