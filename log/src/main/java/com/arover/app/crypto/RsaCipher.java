package com.arover.app.crypto;

import android.util.Base64;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * @author MZY
 * created at 2021/3/13 16:45
 */
public class RsaCipher {
    public static final String RSA = "RSA";
    public static final String TRANSFORMATION = "RSA/NONE/OAEPwithSHA-256andMGF1Padding";

    public static byte[] encrypt(byte[] data, byte[] publicKey) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PublicKey pubKey = keyFactory.generatePublic(keySpec);

        Cipher cp = Cipher.getInstance(TRANSFORMATION);
        cp.init(Cipher.ENCRYPT_MODE, pubKey);
        return cp.doFinal(data);

    }


    public static byte[] decrypt(byte[] encrypted, byte[] privateKey) throws Exception {

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);
        KeyFactory kf = KeyFactory.getInstance(RSA);
        PrivateKey keyPrivate = kf.generatePrivate(keySpec);

        Cipher cp = Cipher.getInstance(TRANSFORMATION);
        cp.init(Cipher.DECRYPT_MODE, keyPrivate);

        return cp.doFinal(encrypted);
    }

    public static KeyPair createKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA);
        kpg.initialize(2048);
        return kpg.genKeyPair();
    }

    public static PublicKey stringToPublicKey(String publicKeyString) throws Exception {

        if (publicKeyString.contains("-----BEGIN PUBLIC KEY-----")
                || publicKeyString.contains("-----END PUBLIC KEY-----")) {

            publicKeyString = publicKeyString
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "");
        }

        byte[] keyBytes = Base64.decode(publicKeyString, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(spec);

    }

}
