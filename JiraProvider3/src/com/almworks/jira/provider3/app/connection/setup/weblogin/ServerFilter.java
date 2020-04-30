package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.jira.provider3.app.connection.JiraConfiguration;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ServerFilter implements Function<DetectedJiraServer, String> {
  private final List<Function<DetectedJiraServer, String>> myFilters = new ArrayList<>();

  @Override
  public String apply(DetectedJiraServer server) {
    if (server == null) return null;
    for (Function<DetectedJiraServer, String> filter : myFilters) {
      String reason = filter.apply(server);
      if (reason != null) return reason;
    }
    return null;
  }

  public ServerFilter addFilter(Function<DetectedJiraServer, String> filter) {
    if (filter != null) myFilters.add(filter);
    return this;
  }

  public ServerFilter checkUrl(String expectedBaseUrl, LocalizedAccessor.MessageStr reason) {
    return addFilter(server -> {
      String serverUri = server.getBaseUri().getBaseUri().toString();
      return isSameBaseUrl(serverUri, expectedBaseUrl) ? null : reason.formatMessage(expectedBaseUrl);
    });
  }

  public ServerFilter checkAccount(String username, String reason) {
    return addFilter(server -> {
      if (Util.NN(username).trim().isEmpty()) {
        if (server.getUsername() == null) return null;
        return reason;
      }
      if (username.equalsIgnoreCase(server.getUsername())) return null;
      return reason;

    });
  }

  @SuppressWarnings("SimplifiableIfStatement")
  private static boolean isSameBaseUrl(String baseUrl, String ownBaseUrl) {
    baseUrl = JiraConfiguration.normalizeBaseUrl(baseUrl);
    if (Objects.equals(ownBaseUrl, baseUrl)) return true;
    if (baseUrl == null || ownBaseUrl == null) return false;
    return ownBaseUrl.equalsIgnoreCase(baseUrl);
  }
}
