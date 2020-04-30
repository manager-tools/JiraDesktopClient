package org.almworks.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Predicate;

public class ExceptionUtil {
  /**
   * Returns stack trace as it would have appeared on stderr.
   *
   * @param throwable exception
   * @return stack trace
   */
  @NotNull
  public static String getStacktrace(Throwable throwable) {
    if (throwable == null)
      return "";
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    throwable.printStackTrace(writer);
    writer.close();
    return out.toString();
  }

  /**
   * Returns exception wrapped to runtime exception. If e is already a runtime exception, returns e.
   *
   * @param e an exception
   * @return a runtime exception
   */
  @NotNull
  public static RuntimeException wrapUnchecked(Throwable e) {
    if (e instanceof RuntimeException)
      return (RuntimeException) e;
    else if (e instanceof InterruptedException)
      return new RuntimeInterruptedException((InterruptedException) e);
    else
      return new Failure(e);
  }

  /**
   * This method will never return. It always throws exception.
   * Can be used as: <p><code>throw ExceptionUtil#rethrow(exception)</code>
   *
   * @return never
   */
  public static RuntimeException rethrow(@NotNull Throwable throwable) {
    if (throwable instanceof Error)
      throw (Error) throwable;
    throw wrapUnchecked(throwable);
  }

  /**
   * Rethrows exception only if it is not null
   */
  public static void rethrowNullable(@Nullable Throwable throwable) {
    if (throwable != null) {
      throw rethrow(throwable);
    }
  }

  /**
   * Gets the cause of invocation target exceptions
   */
  public static Throwable unwrapInvocationException(@Nullable Throwable throwable) {
    while (throwable instanceof InvocationTargetException)
      throwable = throwable.getCause();
    return throwable;
  }

  public static Throwable unwrapCause(@Nullable Throwable throwable) {
    if (throwable == null) return null;
    for (Throwable t = throwable.getCause(); t != null && t != throwable; throwable = t);
    return throwable;
  }
  
  public static <E extends Throwable> void maybeThrow(@Nullable E e) throws E {
    if (e != null) throw e;
  }

  public static Throwable findException(Throwable throwable, Predicate<Throwable> filter) {
    Throwable found = findSuppressedException(throwable, filter);
    if (found != null) return found;
    Throwable cause = throwable;
    while ((cause = cause.getCause()) != null) {
      found = findSuppressedException(cause, filter);
      if (found != null) return found;
    }
    return null;
  }

  private static Throwable findSuppressedException(Throwable throwable, Predicate<Throwable> filter) {
    if (filter.test(throwable)) return throwable;
    for (Throwable suppressed : throwable.getSuppressed()) {
      Throwable found = findException(suppressed, filter);
      if (found != null) return found;
    }
    return null;
  }
}
