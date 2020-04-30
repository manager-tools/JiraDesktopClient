package com.almworks.api.http;

import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;

import java.io.IOException;

public interface HttpLoader {
  String DEFAULT_CHARSET = "ISO-8859-1";

  void setRetries(int retries);

  HttpResponseData load() throws IOException, HttpLoaderException;

  HttpMaterial getMaterial();

  void setRedirectMethodFactory(HttpMethodFactory factory);

  void setFailedStatusApprover(Condition<Integer> failedStatusCodeApprover);

  void setReportAcceptor(HttpReportAcceptor reportAcceptor);

  void addRedirectUriHandler(RedirectURIHandler handler);

  /**
   * Sets condition when the loader should follow redirects.<br>
   * The passed function is called with value of Location HTTP response header. If it then returns false then loader does not follow redirect and immediate return reply with 3xx status code.
   * @param followRedirect condition when loader should follow redirects
   */
  void setFollowRedirect(Function<String, Boolean> followRedirect);

  /**
   * When redirect HTTP status code is returned by server the loader may append original URL query part to redirected URL (if redirected query part is empty).<br>
   * Set this option to false to always go to location returned by server without adding query part from original URL.
   * @param copyQueryOnRedirect true (default) to add query to redirected URL, false to visit it unchanged.
   */
  void setCopyQueryOnRedirect(boolean copyQueryOnRedirect);
}
