package com.almworks.http.errors;

import com.almworks.util.LocalLog;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class URLLoaderExceptionInterceptor extends Handler {
  private static final Level REQUIRED_LEVEL = Level.FINEST;
  private static final LocalLog log = LocalLog.topLevel("webkit.URLLoader");

  private final CopyOnWriteArrayList<Consumer<LogRecord>> myProcessors = new CopyOnWriteArrayList<>();

  public void install(Lifespan life) {
    Logger logger = Logger.getLogger("com.sun.webkit.network.URLLoader");
    Level level = getEffectiveLevel(logger);
    if (level == null || level.intValue() > REQUIRED_LEVEL.intValue()) logger.setLevel(REQUIRED_LEVEL);
    logger.addHandler(this);
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        logger.removeHandler(URLLoaderExceptionInterceptor.this);
      }
    });
  }

  public URLLoaderExceptionInterceptor addProcessor(Consumer<LogRecord> processor) {
    myProcessors.add(processor);
    return this;
  }

  @Nullable
  private static Level getEffectiveLevel(Logger logger) {
    Level level = null;
    while (logger != null && (level = logger.getLevel()) == null) logger = logger.getParent();
    return level;
  }

  @Override
  public void publish(LogRecord record) {
    for (Consumer<LogRecord> processor : myProcessors) processor.accept(record);
    if (!log.isLoggable(Level.INFO)) return;
    Throwable thrown = record.getThrown();
    if (thrown == null) log.debug(record.getLevel(), record.getMessage());
    else log.warning(record.getMessage(), thrown);
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() throws SecurityException {
  }
}
