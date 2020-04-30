package com.almworks.jira.provider3.app.connection.setup;

import com.almworks.api.http.HttpMaterial;
import com.almworks.api.http.HttpUtils;
import com.almworks.http.errors.SNIErrorHandler;
import com.almworks.restconnector.JiraCookies;
import com.almworks.restconnector.JiraCredentials;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.http.WebCookieManager;
import org.almworks.util.Util;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JiraBaseUri {
  @NotNull
  private final URI myBaseUri;

  private JiraBaseUri(@NotNull URI baseUri) {
    myBaseUri = baseUri;
  }

  @NotNull
  public URI getBaseUri() {
    return myBaseUri;
  }

  @NotNull
  public static JiraBaseUri hostPath(URI hostUri, String path) throws URISyntaxException {
    path = Util.NN(path);
    while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
    path += "/";
    String protocol = hostUri.getScheme();
    int port = HttpUtils.removeDefaultPort(protocol, hostUri.getPort());
    return new JiraBaseUri(new URI(protocol, hostUri.getUserInfo(), hostUri.getHost(), port, path, null, null));
  }

  public static List<JiraBaseUri> fromCookie(URI hostUri, List<Cookie> cookies) {
    List<JiraBaseUri> baseUris = Stream.concat(
      WebCookieManager.findCookie(cookies, JiraCookies.JSESSIONID).stream(),
      WebCookieManager.findCookie(cookies, JiraCookies.XSRF_TOKEN).stream())
      .map(Cookie::getPath)
      .distinct()
      .sorted()
      .map(path -> {
        try {
          return JiraBaseUri.hostPath(hostUri, path);
        } catch (URISyntaxException e) {
          LogHelper.error(e);
          return null;
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    baseUris.forEach(baseUri -> LogHelper.debug("Session cookie found:", baseUri.getBaseUri()));
    return baseUris;
  }

  public RestSession createSession(JiraCredentials credentials, HttpMaterial material, SNIErrorHandler sniErrorHandler) {
    return RestSession.create(myBaseUri.toString(), credentials, material, null, sniErrorHandler);
  }

  @Override
  public String toString() {
    return myBaseUri.toString();
  }

  /**
   * Checks if the given other URI locates a resource on the JIRA server - is a descendant of this base URI.
   * @param other URI to check
   * @return true if other URI is descendant
   */
  public boolean isSubUri(URI other) {
    if (other == null) return false;
    if (!other.isAbsolute()) return true;
    if (myBaseUri.isOpaque() != other.isOpaque()) return false;
    String scheme = myBaseUri.getScheme();
    String host = myBaseUri.getHost();
    if (scheme != null && !scheme.equalsIgnoreCase(other.getScheme())) return false;
    if (host != null && !host.equalsIgnoreCase(other.getHost())) return false;
    String basePath = Util.lower(myBaseUri.getPath());
    String otherPath = Util.lower(other.getPath());
    if (basePath != null) {
      if (otherPath == null) return false;
      if (!otherPath.startsWith(basePath)) return false;
    }
    return true;
  }
}
