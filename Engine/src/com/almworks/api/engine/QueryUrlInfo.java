package com.almworks.api.engine;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class QueryUrlInfo {
  @Nullable
  private List<String> myQueryUrls;

  private boolean myValid = true;

  private String myFatalProblem;

  private String myWarning;


  public QueryUrlInfo() {
  }

  public QueryUrlInfo(String url) {
    setUrl(url);
  }

  @Nullable
  public List<String> getQueryUrls() {
    return myQueryUrls;
  }

  public void setQueryUrls(List<String> queryUrls) {
    myQueryUrls = queryUrls == null ? null : Collections15.arrayList(queryUrls);
  }

  public boolean isValid() {
    return myValid;
  }

  public void setValid(boolean valid) {
    myValid = valid;
  }

  @Nullable
  public String getFatalProblem() {
    return myFatalProblem;
  }

  public void setFatalProblem(String fatalProblem) {
    myFatalProblem = fatalProblem;
  }

  /**
   * @return warning html
   */
  @Nullable
  public String getWarning() {
    return myWarning;
  }

  public void setWarning(String warning) {
    myWarning = warning;
  }

  public void setUrl(String url) {
    myQueryUrls = Collections.singletonList(url);
  }
}
