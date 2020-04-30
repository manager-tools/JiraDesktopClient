package com.almworks.util.files;

import org.almworks.util.Log;

import java.net.URLEncoder;

public class UrlUtil {

  public static String javaEncodeParameter(String subject) {
    try {
      subject = URLEncoder.encode(subject);
      subject = subject.replaceAll("\\+", "%20");
    } catch (Exception e) {
      // ignore
      Log.warn(e);
    }
    return subject;
  }

  public static String getMailtoUrl(String email, String subject, String body) {
    String url = "mailto:" + email + "?subject=" + javaEncodeParameter(subject);
    if (body != null) {
      body = body.replaceAll("\\<br\\>", "\n");
      url += "&body=" + javaEncodeParameter(body);
    }
    return url;
  }
}
