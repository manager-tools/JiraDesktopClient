package com.almworks.restconnector.login;

import com.almworks.api.engine.Connection;
import com.almworks.util.LocalLog;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.http.CookieIdentityComparator;
import com.almworks.util.http.WebCookieManager;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.Computable;
import org.almworks.util.Collections15;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This container service implements a register of alive sessions of {@link JiraAccount JIRA accounts}.<br>
 * The application may have several sessions to the same account. This happens because of user creates a new session while
 * creating a new JIRA connection.<br>
 * The intention of this register is provide help to reuse alive sessions among several connections to the same JIRA account.
 * This may help user to restore expired sessions. Since with the help of this service, connection may reuse a session
 * already restored by another connection with out disturbing user with relogin.<br>
 * <br><b>Usage of this service</b><br>
 * <ul>
 *   <li>
 *      When a connection has successfully accessed JIRA server, it must inform this register by calling {@link #markConfirmed(JiraAccount, List)}.
 *      The connection should do this on any successful access, however it may do it only once if several server requests are sent in a short time.
 *   </li>
 *   <li>
 *      When connection detects session expiration it must immediately inform the register by calling {@link #markFailed(String, List)}
 *   </li>
 *   <li>
 *      The special case is relogin: it must inform the register about successful session restoration until releasing
 *      {@link com.almworks.api.engine.GlobalLoginController#updateLogin(Connection, Computable) update login lock}.
 *      Thus other connection to the same account gets a chance to reuse already restored session as soon as it acquires the lock.
 *      </li>
 * </ul>
 * When connection needs to restore it's session it should ask the register to {@link #suggestCookies(JiraAccount)} for it's account.
 */
public class AuthenticationRegister {
  private static final LocalLog log = LocalLog.topLevel("JiraAuthReg");
  public static final Role<AuthenticationRegister> ROLE = Role.role(AuthenticationRegister.class);
  private final MultiMap<JiraAccount, Record> myConfirmed = new MultiMap<>();

  public void markConfirmed(JiraAccount account, List<Cookie> cookies) {
    if (!account.isKnow()) return;
    log.debug("Cookies confirmed", account, cookies);
    cookies = new ArrayList<>(cookies);
    Collections.sort(cookies, CookieIdentityComparator.INSTANCE);
    synchronized (myConfirmed) {
      removeRecord(account, cookies);
      myConfirmed.add(account, new Record(cookies, System.currentTimeMillis()));
    }
  }

  public void markFailed(String baseUrl, List<Cookie> cookies) {
    baseUrl = JiraAccount.normalize(baseUrl);
    cookies = new ArrayList<>(cookies);
    Collections.sort(cookies, CookieIdentityComparator.INSTANCE);
    log.debug("Cookies failed. Start clean", baseUrl, cookies);
    synchronized (myConfirmed) {
      for (JiraAccount account : new ArrayList<>(myConfirmed.keySet())) {
        if (Objects.equals(baseUrl, account.getBaseUrl())) {
          log.debug("Clean: account found", account);
          removeRecord(account, cookies);
        }
      }
    }
    log.debug("Cookies failed. End clean", baseUrl, cookies);
  }

  @Nullable
  public List<Cookie> suggestCookies(JiraAccount account) {
    if (!account.isKnow()) {
      LogHelper.error("Cannot suggest for unknown account", account);
      return null;
    }
    synchronized (myConfirmed) {
      List<Record> records = myConfirmed.getAll(account);
      if (records == null) return null;
      Record best = null;
      for (Record record : records) {
        if (best == null) best = record;
        else if (best.myLastConfirmed < record.myLastConfirmed) best = record;
      }
      return best != null ? Collections15.unmodifiableListCopy(best.myCookies) : null;
    }
  }

  private void removeRecord(JiraAccount account, List<Cookie> cookies) {
    assert Thread.holdsLock(myConfirmed);
    List<Record> records = myConfirmed.getAll(account);
    if (records != null) {
      Record known = records.stream().filter(record -> record.isSame(cookies)).findFirst().orElse(null);
      if (known != null) {
        log.debug("Removing record", account, known);
        myConfirmed.remove(account, known);
      } else log.debug("Remove record: not found", account, cookies);
    } else log.debug("Remove record: not records for account", account);
  }

  private static class Record {
    private final List<Cookie> myCookies;
    private final long myLastConfirmed;

    public Record(List<Cookie> cookies, long lastConfirmed) {
      myCookies = cookies;
      myLastConfirmed = lastConfirmed;
    }

    public boolean isSame(List<Cookie> cookies) {
      return WebCookieManager.areSame(myCookies, cookies);
    }

    @Override
    public String toString() {
      String duration = DateUtil.getFriendlyDurationVerbose((int) ((System.currentTimeMillis() - myLastConfirmed) / 1000));
      return String.format("AuthRecord[%s (%s), %s]", duration, Instant.ofEpochMilli(myLastConfirmed), myCookies);
    }
  }
}
