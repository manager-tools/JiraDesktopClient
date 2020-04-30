package com.almworks.api.http;

import org.apache.commons.httpclient.HttpMethodBase;

public interface HttpMethodFactory {
  HttpMethodBase create() throws HttpMethodFactoryException;
}
