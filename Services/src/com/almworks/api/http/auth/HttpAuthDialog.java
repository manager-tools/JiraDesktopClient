package com.almworks.api.http.auth;

import com.almworks.util.Pair;
import com.almworks.util.properties.Role;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface HttpAuthDialog {
  Role<HttpAuthDialog> ROLE = Role.role("httpAuthDialog");

  Pair<HttpAuthCredentials, HttpAuthPersistOption> show(HttpAuthChallengeData data, HttpAuthCredentials failed,
    boolean proxy);
}
