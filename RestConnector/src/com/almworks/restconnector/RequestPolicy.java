package com.almworks.restconnector;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntList;
import org.jetbrains.annotations.Nullable;

public class RequestPolicy {
  /**
   * @see #myRetryOnServerFailure
   */
  public static final IntList HTTP_FAILURES = IntArray.create(504);

  /**
   * Policy for requests that can be safely retried because of they do not modify server state.
   */
  public static final RequestPolicy SAFE_TO_RETRY = new RequestPolicy(Boolean.TRUE, 1);

  /**
   * Retry on {@link #HTTP_FAILURES server failures}, mark that login is required but don't allow to retry operation if wrong login state detected
   */
  public static final RequestPolicy NEEDS_LOGIN = new RequestPolicy(Boolean.FALSE, 1);

  /**
   * Operation doesn't require logged in state to succeed, allows to retry on {@link #HTTP_FAILURES server failures}.
   */
  public static final RequestPolicy FAILURE_ONLY = new RequestPolicy(null, 1);

  /**
   * If true checks response header X-AUSERNAME and compare it to expected user login. If login do not match (and authenticated connection is expected) performs login and retry the request.
   */
  @Nullable
  private final Boolean myCheckLogin;

  /**
   * Number of attempts to resend request when get {@link #HTTP_FAILURES HTTP} or IP failure.<br><br>
   * This number does not count retries occurred due to other policy settings, for example if this property is set to 1 and {@link #myCheckLogin checkLogin} is set to true the following
   * request sequence is possible:<br>
   * 1. Request and get failure.<br>
   * 2. Retry and get wrong login (1 retry after failure is allowed by this field)<br>
   * 3. Perform login<br>
   * 4. Another retry (retry after re-login)
   *
   */
  private final int myRetryOnServerFailure;

  public RequestPolicy(@Nullable Boolean checkLogin, int retryOnServerFailure) {
    myCheckLogin = checkLogin;
    myRetryOnServerFailure = retryOnServerFailure;
  }

  /**
   * @return true means that it's safe to retry operation. Client code may check logged in state after first attempt and if session is not logged in than retry operation.
   * @see #isNeedsLogin()
   */
  public boolean isCheckLogin() {
    return Boolean.TRUE.equals(myCheckLogin);
  }

  /**
   * @return true means that operation depends on session's login state. However this flag does not allow to retry.
   * @see #isCheckLogin()
   */
  public boolean isNeedsLogin() {
    return myCheckLogin != null;
  }

  public int getRetryOnServerFailure() {
    return myRetryOnServerFailure;
  }
}
