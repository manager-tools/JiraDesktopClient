package com.almworks.passman;

import com.almworks.api.passman.PMCredentials;
import com.almworks.api.passman.PMDomain;
import com.almworks.api.store.StoreFeature;
import org.almworks.util.Util;

public class PasswordManagerTests extends PasswordManagerFixture {
  public void testMemory() {
    PMDomain domain = new PMDomain("basic", "www.haba.ru", 80, "haba");
    PMCredentials creds = myPassman.loadCredentials(domain);
    assertNull(creds);

    creds = new PMCredentials("user", "pass");
    myPassman.saveCredentials(domain, creds, false);
    PMCredentials creds2 = myPassman.loadCredentials(domain);
    assertEqualCredentials(creds, creds2, "basic");

    // nothing is written
    assertNull(myStore.access(PasswordManagerImpl.STORE_KEY, StoreFeature.SECURE_STORE).load());
  }

  public void testDisk() throws InterruptedException {
    PMDomain domain = new PMDomain("basic", "www.haba.ru", 80, "haba");
    PMCredentials creds = myPassman.loadCredentials(domain);
    assertNull(creds);

    creds = new PMCredentials("user", "pass");
    myPassman.saveCredentials(domain, creds, true);
    PMCredentials creds2 = myPassman.loadCredentials(domain);
    assertEqualCredentials(creds, creds2, "basic");

    // writing is delayed
    Thread.sleep(100);
    assertNotNull(myStore.access(PasswordManagerImpl.STORE_KEY, StoreFeature.SECURE_STORE).load());
  }

  private void assertEqualCredentials(PMCredentials c1, PMCredentials c2, String domain) {
    assertEquals(c1.getUsername(), c2.getUsername());
    assertEquals(c1.getPassword(), c2.getPassword());
    String a1 = c1.getAuthType();
    String a2 = c2.getAuthType();
    assertTrue(a1 + " " + a2,
      Util.equals(a1, a2) || (a1 == null && Util.equals(a2, domain)) || ((a2 == null && Util.equals(a1, domain))));
  }

  public void testDiskMemoryConflict() {
    PMDomain domain = new PMDomain("basic", "www.haba.ru", 80, "haba");
    PMCredentials creds1 = new PMCredentials("user", "pass1");
    myPassman.saveCredentials(domain, creds1, false);

    PMCredentials creds2 = new PMCredentials("user", "pass2");
    myPassman.saveCredentials(domain, creds2, true);

    assertEqualCredentials(creds2, myPassman.loadCredentials(domain), "basic");
  }

  public void testMemoryDiskConflict() {
    PMDomain domain = new PMDomain("basic", "www.haba.ru", 80, "haba");
    PMCredentials creds1 = new PMCredentials("user", "pass1");
    myPassman.saveCredentials(domain, creds1, true);

    PMCredentials creds2 = new PMCredentials("user", "pass2");
    myPassman.saveCredentials(domain, creds2, false);

    assertEqualCredentials(creds2, myPassman.loadCredentials(domain), "basic");
  }
}
