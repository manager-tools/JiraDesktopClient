package com.almworks.spi.provider.util;

import com.almworks.api.http.FeedbackHandler;
import com.almworks.api.http.HttpCancelledException;
import com.almworks.api.http.HttpUtils;
import com.almworks.api.http.auth.HttpAuthChallengeData;
import com.almworks.api.http.auth.HttpAuthCredentials;
import com.almworks.api.http.auth.HttpAuthPersistOption;
import com.almworks.api.passman.PMCredentials;
import com.almworks.api.passman.PMDomain;
import com.almworks.api.passman.PasswordManager;
import com.almworks.util.Pair;
import org.almworks.util.Log;
import org.almworks.util.Util;

public abstract class AbstractFeedbackHandler implements FeedbackHandler {
  private final Object myLock = new Object();
  protected final PasswordManager myPasswordManager;

  protected AbstractFeedbackHandler(PasswordManager passwordManager) {
    myPasswordManager = passwordManager;
  }

  public final HttpAuthCredentials requestCredentials(HttpAuthChallengeData data, HttpAuthCredentials failed,
    boolean quiet) throws InterruptedException, HttpCancelledException
  {
    Log.debug("AFH: called for " + data);
    // avoid reentrance, because we can't correctly reply with credentials while other credentials are being entered
    synchronized (myLock) {
      Log.debug("AFH: going for " + data);
      PMDomain domain = new PMDomain(data.getAuthScheme(), data.getHost(), data.getPort(), data.getRealm());
      PMCredentials stored = myPasswordManager.loadCredentials(domain);
      if (stored != null && !equalCredentials(data.getAuthScheme(), failed, stored)) {
        String username = PMCredentials.USERNAME.get(stored.getMap());
        String password = PMCredentials.PASSWORD.get(stored.getMap());
        Log.debug("AFH: found user " + username + " for " + data);
        return new HttpAuthCredentials(username, password);
      }

      Pair<HttpAuthCredentials, HttpAuthPersistOption> result;
      if (quiet) {
        Log.debug("AFH: quiet mode, not requesting user for " + data);
        result = null;
      } else {
        Log.debug("AFH: requesting user for " + data);
        result = doRequest(domain, data, failed, data.isProxy());
      }
      HttpAuthCredentials creds = result == null ? null : result.getFirst();
      Log.debug(creds == null ? "AFH: no creds from user" : "AFH: got user " + creds.getUsername());

      HttpAuthPersistOption persistOption = result == null ? HttpAuthPersistOption.DONT_KEEP : result.getSecond();
      if (persistOption != HttpAuthPersistOption.DONT_KEEP && creds != null) {
        boolean saveOnDisk = persistOption == HttpAuthPersistOption.KEEP_ON_DISK;
        PMCredentials pmcreds = new PMCredentials(creds.getUsername(), creds.getPassword());
        Log.debug("AFH: saving creds (" + saveOnDisk + ")");
        myPasswordManager.saveCredentials(domain, pmcreds, saveOnDisk);
      }

      return creds;
    }
  }

  public Pair<HttpAuthCredentials, String> requestPreliminaryCredentials(String host, int port, boolean proxy) {
    Log.debug("AFH: called for preliminary [" + host + ":" + port + ":" + proxy + "]");
    synchronized (myLock) {
      PMDomain domain = new PMDomain(host, port);
      PMCredentials stored = myPasswordManager.loadCredentials(domain);
      if (stored == null) {
        Log.debug("AFH: no auth");
        return null;
      }
      String username = stored.getUsername();
      String password = stored.getPassword();
      if (username == null || password == null) {
        Log.debug("AFH: bad data");
        return null;
      }
      String authType = Util.NN(stored.getAuthType(), "Basic");
      if (!"Basic".equalsIgnoreCase(authType)) {
        Log.debug("AFH: non-basic authtype, no preliminary");
        return null;
      }
      HttpAuthCredentials r = new HttpAuthCredentials(username, password);
      Log.debug("AFH: preliminary auth: " + r);
      return Pair.create(r, authType);
    }
  }

  protected abstract Pair<HttpAuthCredentials, HttpAuthPersistOption> doRequest(PMDomain domain,
    HttpAuthChallengeData data, HttpAuthCredentials failed, boolean proxy)
    throws HttpCancelledException, InterruptedException;

  protected static boolean equalCredentials(String authScheme, HttpAuthCredentials c1, PMCredentials c2) {
    if (c1 == null || c2 == null)
      return false;
    if (!Util.equals(c1.getPassword(), PMCredentials.PASSWORD.get(c2.getMap())))
      return false;
    String user1 = c1.getUsername();
    String user2 = PMCredentials.USERNAME.get(c2.getMap());
    if ("NTLM".equalsIgnoreCase(authScheme)) {
      if (!HttpUtils.areNTUsernamesEquals(user1, user2, true))
        return false;
    } else {
      if (!Util.equals(user1, user2))
        return false;
    }
    return true;
  }
}