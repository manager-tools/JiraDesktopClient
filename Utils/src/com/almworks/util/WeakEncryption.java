package com.almworks.util;

import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.security.GeneralSecurityException;

/**
 * :todoc:
 *
 * @author sereda
 */
public class WeakEncryption {
  /**
   * Originally copied from Pub_ALMW, 16.02.2005
   */
  public static final String ALGORITHM = "TripleDES";
  public static final byte[] myKey = {
    -56, -122, -20, -80, 56, -63, 79, 41, 70, -26, -123, -33, 124, -14, 1, -60,
    -56, -122, -20, -80, 56, -63, 79, 41,
  };

  private WeakEncryption() {
  }

  public static byte[] decrypt(String encrypted) throws GeneralSecurityException {
    if (encrypted == null)
      return null;
    byte[] binaryData = Base64.decodeBase64(encrypted.getBytes());
    binaryData = decrypt(binaryData);
    return binaryData;
  }

  public static byte[] decrypt(byte[] data) throws GeneralSecurityException {
    if (data == null || data.length == 0)
      return data;
    Cipher cipher = prepare(Cipher.DECRYPT_MODE);
    return cipher.doFinal(data);
  }

  public static String decryptString(String encrypted) throws GeneralSecurityException {
    byte[] binaryData = decrypt(encrypted);
    if (binaryData == null)
      return null;
    return new String(binaryData);
  }

  public static byte[] encrypt(byte[] data) {
    if (data == null || data.length == 0)
      return data;
    Cipher cipher = prepare(Cipher.ENCRYPT_MODE);
    try {
      byte[] result = cipher.doFinal(data);
      return result;
    } catch (GeneralSecurityException e) {
      throw new Failure(e);
    }
  }

  private static Cipher prepare(int mode) {
    Cipher cipher;
    try {
      SecretKeyFactory secretKeyFactory = null;
      try {
        secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
      } catch (Exception e) {
        Log.error(ALGORITHM + " is not supported on your system, cannot continue. Please check your JRE or contact support.");
        throw new Failure(e);
      }
      SecretKey key = secretKeyFactory.generateSecret(new DESedeKeySpec(myKey));
      cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(mode, key);
    } catch (GeneralSecurityException e) {
      throw new Failure(e);
    }
    return cipher;
  }

  public static String encryptString(byte[] data) {
    if (data == null)
      return null;
    data = encrypt(data);
    data = Base64.encodeBase64(data);
    return new String(data);
  }

  public static String encryptString(String unencrypted) {
    if (unencrypted == null)
      return null;
    byte[] binaryData = unencrypted.getBytes();
    return encryptString(binaryData);
  }
}
