package com.almworks.restconnector.operations;

import com.almworks.api.connector.ConnectorException;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LocalLog;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RestAuth1Session {
  public static final JSONKey<String> USER_ACCOUNT_ID = JSONKey.textLower("accountId");
  private static final LocalLog log = LocalLog.topLevel("rest.1.auth");

  /** No response was received */
  public static final int R_NO_RESPONSE = 0;
  /** Wrong format. Server responded something, but the response cannot be parsed. */
  public static final int R_WRONG = 1;
  /** Response was obtained and successfully parsed */
  public static final int R_OK = 2;

  /** Contains connection problem if request was failed to perform. */
  @Nullable
  private final ConnectorException myFailure;
  /** Result status, see R_* constants for explanation */
  private final int myResult;
  private final LoadUserInfo myUserInfo;

  private RestAuth1Session(@Nullable ConnectorException failure, int result, @Nullable LoadUserInfo userInfo) {
    myFailure = failure;
    myResult = result;
    myUserInfo = userInfo;
  }

  public int getResult() {
    return myResult;
  }

  /**
   * @return true if proper response obtained. The session may be authenticated or anonymous
   */
  public boolean hasUsername() {
    return myResult == R_OK;
  }

  /**
   * Network failure. If this method returns null this means just some response is obtained, but it still can be invalid
   * and contain no session info.
   * @return network problem or null if no network problem occurred
   */
  @Nullable
  public ConnectorException getFailure() {
    return myFailure;
  }

  public ConnectorException getFailureOr(@NotNull ConnectorException notAuthenticated) {
    if (hasUsername()) log.error("Not failed");
    return myFailure != null ? myFailure : notAuthenticated;
  }

  private static RestAuth1Session error(ConnectorException failure, int result) {
    assert result != R_OK;
    assert failure != null;
    return new RestAuth1Session(failure, result, null);
  }

  /**
   * Applicable if {@link #hasUsername()} returns true (which is equal to {@link #getResult()} == {@link #R_OK})
   * @return null for anonymous session, not null session's account
   */
  @Nullable
  public LoadUserInfo getUserInfo() {
    return myUserInfo;
  }

  public static RestAuth1Session get(RestSession session, RequestPolicy policy, boolean auxiliary) {
    LoadUserInfo userInfo;
    try {
      userInfo = LoadUserInfo.loadMe(session, policy, auxiliary);
    } catch (ConnectorException e) {
      return error(e, R_NO_RESPONSE);
    }
    if (userInfo.getDisplayName() == null || userInfo.getAccountId() == null) {
      LogHelper.error("myself resource misses accountId or displayName:", userInfo);
      return error(null, R_WRONG);
    }
    return new RestAuth1Session(null, R_OK, userInfo);
  }
}
