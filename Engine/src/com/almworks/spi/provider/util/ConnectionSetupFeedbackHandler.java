package com.almworks.spi.provider.util;

import com.almworks.api.http.HttpCancelledException;
import com.almworks.api.http.auth.HttpAuthChallengeData;
import com.almworks.api.http.auth.HttpAuthCredentials;
import com.almworks.api.http.auth.HttpAuthPersistOption;
import com.almworks.api.passman.PMDomain;
import com.almworks.api.passman.PasswordManager;
import com.almworks.util.Pair;
import org.almworks.util.Util;

public abstract class ConnectionSetupFeedbackHandler extends AbstractFeedbackHandler {
  private boolean myHadError = false;

  public ConnectionSetupFeedbackHandler(PasswordManager passwordManager) {
    super(passwordManager);
  }

  protected Pair<HttpAuthCredentials, HttpAuthPersistOption> doRequest(PMDomain domain, HttpAuthChallengeData data,
    HttpAuthCredentials failed, boolean proxy) throws HttpCancelledException, InterruptedException
  {
    String scheme = Util.upper(Util.NN(data.getAuthScheme()));
    if (scheme.length() > 0)
      scheme += " ";
    String message = (proxy ? "PROXY " : "HTTP ") + scheme + "authentication requested";
    if (failed != null) {
      message += " (tried username '" + failed.getUsername() + "'";
      if (Util.NN(failed.getPassword()).length() > 0)
        message += " with a password";
      message += ")";
    }
    setError(message);
    myHadError = true;
    throw new HttpCancelledByFeedbackException(message);
  }

  protected void setError(String message) {
  }

  public boolean hadError() {
    return myHadError;
  }

}
