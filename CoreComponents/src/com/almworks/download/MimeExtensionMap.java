package com.almworks.download;

import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Map;

class MimeExtensionMap {
  private static final String[] KNOWN_EXTENSIONS = {
    "zip", "gz", "exe", "lib", "pdf", "rtf", "ps", "latex", "tex", "sh", "cgi", "tar",
    "wav", "gif", "jpg", "tiff", "png", "html", "txt", "mpeg", "mov", "avi", "xml"
  };

  private static final String[] KNOWN_MAPPINGS = {
    "image/bitmap", "bmp",
  };

  private final Map<String, String> myMimeTypeToExtensionMap = Collections15.hashMap();

  private static MimeExtensionMap ourInstance = null;

  private MimeExtensionMap() {
    FileNameMap map = URLConnection.getFileNameMap();
    for (int i = 0; i < KNOWN_EXTENSIONS.length; i++) {
      String extension = KNOWN_EXTENSIONS[i];
      String name = "x." + extension;
      String type = map.getContentTypeFor(name);
      if (type == null)
        continue;
      if (myMimeTypeToExtensionMap.containsKey(type))
        myMimeTypeToExtensionMap.put(type, null);
      else
        myMimeTypeToExtensionMap.put(type, extension);
    }
    for (int i = 0; i < KNOWN_MAPPINGS.length - 1; i += 2) {
      String mime = KNOWN_MAPPINGS[i];
      String ext = KNOWN_MAPPINGS[i + 1];
      assert mime.indexOf('/') >= 0;
      myMimeTypeToExtensionMap.put(mime, ext);
    }
  }

  /**
   * Guesses file extension based on the mime type.
   * @return extension without ".", or null
   */
  public static synchronized String guessExtension(String mimeType, String defaultExtension) {
    if (ourInstance == null)
      ourInstance = new MimeExtensionMap();
    return Util.NN(ourInstance.myMimeTypeToExtensionMap.get(mimeType), defaultExtension);
  }
}
