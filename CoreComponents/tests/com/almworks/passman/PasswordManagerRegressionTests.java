package com.almworks.passman;

import com.almworks.api.passman.PMCredentials;
import com.almworks.api.passman.PMDomain;

public class PasswordManagerRegressionTests extends PasswordManagerFixture {
  public void testFederico() {
    PMDomain domain = new PMDomain("basic", "jira-ied2.ms.com", 9020, "prod.ms.dist-releng.jira-/ [00:00:00:0000]");
    PMCredentials credentials = new PMCredentials("user", "incorrect");
    myPassman.saveCredentials(domain, credentials, true);

    PMDomain prelim = new PMDomain("jira-ied2.ms.com", 9020);
    assertEquals("incorrect", myPassman.loadCredentials(prelim).getPassword());

    domain = new PMDomain("basic", "jira-ied2.ms.com", 9020, "prod.ms.dist-releng.jira-/ [00:00:00:0001]");
    credentials = new PMCredentials("user", "correct");
    myPassman.saveCredentials(domain, credentials, false);

    assertEquals("correct", myPassman.loadCredentials(prelim).getPassword());
  }

  public void testFederico2() {
    for (int i = 0; i < 100; i++) {
      PMDomain domain = new PMDomain("basic", "jira-ied2.ms.com", 9020, "prod.ms.dist-releng.jira-/ [" + i + "]");
      PMCredentials credentials = new PMCredentials("user", "incorrect");
      myPassman.saveCredentials(domain, credentials, false);
    }

    PMDomain prelim = new PMDomain("jira-ied2.ms.com", 9020);
    assertEquals("incorrect", myPassman.loadCredentials(prelim).getPassword());

    PMDomain domain = new PMDomain("basic", "jira-ied2.ms.com", 9020, "prod.ms.dist-releng.jira-/ [1134131311313]");
    PMCredentials credentials = new PMCredentials("user", "correct");
    myPassman.saveCredentials(domain, credentials, false);

    assertEquals("correct", myPassman.loadCredentials(prelim).getPassword());
  }

  public void testFederico2_ondisk() {
    for (int i = 0; i < 100; i++) {
      PMDomain domain = new PMDomain("basic", "jira-ied2.ms.com", 9020, "prod.ms.dist-releng.jira-/ [" + i + "]");
      PMCredentials credentials = new PMCredentials("user", "incorrect");
      myPassman.saveCredentials(domain, credentials, true);
    }

    PMDomain prelim = new PMDomain("jira-ied2.ms.com", 9020);
    assertEquals("incorrect", myPassman.loadCredentials(prelim).getPassword());

    PMDomain domain = new PMDomain("basic", "jira-ied2.ms.com", 9020, "prod.ms.dist-releng.jira-/ [1134131311313]");
    PMCredentials credentials = new PMCredentials("user", "correct");
    myPassman.saveCredentials(domain, credentials, true);

    assertEquals("correct", myPassman.loadCredentials(prelim).getPassword());
  }
}
