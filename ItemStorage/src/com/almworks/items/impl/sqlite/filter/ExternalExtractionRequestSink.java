package com.almworks.items.impl.sqlite.filter;

import com.almworks.util.threads.ThreadSafe;

public interface ExternalExtractionRequestSink {
  @ThreadSafe
  void requestProcessing(boolean rebuild);
}