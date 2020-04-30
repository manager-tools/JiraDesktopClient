package com.almworks.util;

import com.almworks.util.tests.BaseTestCase;

import java.security.GeneralSecurityException;

/**
 * :todoc:
 *
 * @author sereda
 */
public class WeakEncryptionTests extends BaseTestCase {
  public void testStringEncryption() throws GeneralSecurityException {
    String shortString = "haba";
    String encrypted = WeakEncryption.encryptString(shortString);
    assertNotSame(shortString, encrypted);
    String decrypted = WeakEncryption.decryptString(encrypted);
    assertEquals(shortString, decrypted);

    String longString = "    String encrypted = WeakEncryption.encryptString(shortString);\n" +
      "    assertNotSame(shortString, encrypted);\n" +
      "    String decrypted = WeakEncryption.decryptString(encrypted);\n" +
      "    assertEquals(shortString, decrypted);";
    encrypted = WeakEncryption.encryptString(longString);
    assertNotSame(longString, encrypted);
    decrypted = WeakEncryption.decryptString(encrypted);
    assertEquals(longString, decrypted);
  }
}
