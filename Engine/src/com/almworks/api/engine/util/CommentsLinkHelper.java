package com.almworks.api.engine.util;

public class CommentsLinkHelper {
  public static final int SUBHASH_MAGNITUDE = 16;
  
  // prime numbers
  private static final int SUBHASH1_MOD = 1039;
  private static final int SUBHASH2_MOD = 3037;
  private static final int SUBHASH3_MOD = 5039;

  /**
   * Gets an almost-hash-value of a string that is stripped of all spaces and non-letter or digit characters.
   */
  public static Integer getCommentHash(String commentText) {
    assert commentText != null;
    if (commentText == null)
      return 0;
    int hash = 2719;
    char[] chars = commentText.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (Character.isLetterOrDigit(c)) {
        hash = hash * 31 + Character.toUpperCase(c);
      }
    }
    // this is taken from HashMap.java
    hash += ~(hash << 9);
    hash ^= (hash >>> 14);
    hash += (hash << 4);
    hash ^= (hash >>> 10);
    return hash;
  }

  public static int subHash1(int commentHash) {
    if (commentHash < 0)
      commentHash = -commentHash;
    return (commentHash % SUBHASH1_MOD) % SUBHASH_MAGNITUDE;
//    return (commentHash & 0xF);
  }

  public static int subHash2(int commentHash) {
    if (commentHash < 0)
      commentHash = -commentHash;
    return (commentHash % SUBHASH2_MOD) % SUBHASH_MAGNITUDE;
//    return (commentHash & 0xF0) >>> 4;
  }

  public static int subHash3(int commentHash) {
    if (commentHash < 0)
      commentHash = -commentHash;
    return (commentHash % SUBHASH3_MOD) % SUBHASH_MAGNITUDE;
//    return (commentHash & 0xF00) >>> 8;
  }
}
