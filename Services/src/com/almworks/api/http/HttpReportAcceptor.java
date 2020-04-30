package com.almworks.api.http;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.URI;

public interface HttpReportAcceptor {
  void report(String method, URI uri, HttpVersion version, Header[] requestHeaders, StatusLine response,
    Header[] responseHeaders);
}
