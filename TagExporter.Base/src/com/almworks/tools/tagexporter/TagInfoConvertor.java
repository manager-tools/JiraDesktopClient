package com.almworks.tools.tagexporter;

import com.almworks.util.collections.Convertor;
import com.almworks.util.tags.TagFileStorage;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.almworks.util.Collections15.arrayList;

public class TagInfoConvertor extends Convertor<TagInfo, TagFileStorage.TagInfo> {
  private final Map<String, String> myConnectionIdUrl;

  public TagInfoConvertor(Map<String, String> connectionIdUrl) {
    myConnectionIdUrl = connectionIdUrl;
  }

  @Override
  public TagFileStorage.TagInfo convert(TagInfo tagInfo) {
    List<String> itemUrls = arrayList(tagInfo.items.size());
    for (String connectionId : tagInfo.items.keySet()) {
      String connectionUrl = myConnectionIdUrl.get(connectionId);
      // We need to normalize URLs, because connection configuration may contain verbatim user input (which may contain but be not equal to base URL)
      String normalizedConnectionUrl = tagInfo.isJira ? normalizeJiraUrl(connectionUrl) : normalizeBugzillaUrl(connectionUrl);
      if (normalizedConnectionUrl == null) continue;
      //noinspection ConstantConditions
      for (String itemId : tagInfo.items.getAll(connectionId)) {
        itemUrls.add(tagInfo.isJira ? getJiraUrl(normalizedConnectionUrl, itemId) : getBugzillaUrl(normalizedConnectionUrl, itemId));
      }
    }
    return new TagFileStorage.TagInfo(tagInfo.name, tagInfo.iconPath, itemUrls);
  }

  private static String getJiraUrl(String normalizedJiraBaseUrl, String issueKey) {
    return normalizedJiraBaseUrl + "/browse/" + issueKey;
  }

  private static String getBugzillaUrl(String normalizedBugzillaBaseUrl, String bugId) {
    return normalizedBugzillaBaseUrl + "show_bug.cgi?id=" + bugId;
  }

  // Copied from BugzillaIntegration.normalizeUrl (tracker2 r6062)
  // modifications:
  // - null is returned instead of throwing MalformedURLException
  // - no checks via GetMethod (if connection was configured with malformed URL, the user hardly could tag any item from it)
  @Nullable
  private static String normalizeBugzillaUrl(String urlString) {
    if (urlString == null) return null;
    urlString = urlString.trim();
    if (urlString.length() == 0) return null;

    try {
    URL url = null;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      if (urlString.indexOf("://") < 0) {
        urlString = "http://" + urlString;
        url = new URL(urlString);
      } else {
        throw e;
      }
    }

    String path = url.getPath();
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1);
    if (path.endsWith(".cgi")) {
      int k = path.lastIndexOf('/');
      if (k >= 0)
        path = path.substring(0, k);
    }
    url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
    String result = url.toExternalForm();
    if (!result.endsWith("/"))
      result = result + "/";

    return result;
    } catch (MalformedURLException e) {
      return null;
    }
  }


  // Copied from JiraUtils.normalizeJiraUrl (tracker2 r6062)
  // modified to return null in case of a malformed URL
  @Nullable
  private static String normalizeJiraUrl(String baseUrl) {
    if (baseUrl == null) return null;
    baseUrl = baseUrl.trim();
    if (baseUrl.length() == 0) return null;
    URL url;
    try {
      try {
        url = new URL(baseUrl);
      } catch (MalformedURLException e) {
        if (baseUrl.indexOf("://") < 0) {
          baseUrl = "http://" + baseUrl;
          url = new URL(baseUrl);
        } else {
          throw e;
        }
      }
      String host = url.getHost();
      if (host == null || host.length() == 0) {
        throw new MalformedURLException("no host");
      }
      String protocol = url.getProtocol();
      if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
        throw new MalformedURLException("unsupported protocol " + protocol);
      }
      String path = url.getPath();
      int k = path.lastIndexOf("/browse");
      if (k != -1) {
        path = path.substring(0, k);
      }
      k = path.lastIndexOf("/secure");
      if (k != -1) {
        path = path.substring(0, k);
      }
      url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
      String result = url.toExternalForm();
      while (result.endsWith("/"))
        result = result.substring(0, result.length() - 1);
      return result;
    } catch (MalformedURLException e) {
      return null;
    }
  }
}
