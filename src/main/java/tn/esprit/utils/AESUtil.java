package tn.esprit.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtil {
    private static final String KEY = "MentorshipKey123";

    public static String encrypt(String value) {
        if (value == null || value.trim().isEmpty())
            return value;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            System.err.println("Encryption failed: " + ex.getMessage());
        }
        return value;
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.trim().isEmpty())
            return encrypted;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(original, "UTF-8");
        } catch (Exception ex) {
            return encrypted; // Fallback or unencrypted
        }
    }
}
