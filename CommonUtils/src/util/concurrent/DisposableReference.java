package util.concurrent;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class DisposableReference<T> {
  private static final Object DISPOSED = new Object();
  private final AtomicReference<Object> myHolder;

  public DisposableReference() {
    this(null);
  }

  public DisposableReference(T reference) {
    myHolder = new AtomicReference<Object>(reference);
  }

  /**
   * @return reference if is set and not yet disposed
   */
  @Nullable
  public T get() {
    Object obj = myHolder.get();
    return obj != DISPOSED ? (T)obj : null;
  }

  /**
   * Sets new reference if not yet set and not disposed
   * @param reference
   * @return reference if not initialized before<br>
   * previously set reference if initialized before and not yet disposed<br>
   * null if already disposed or not initialized and parameter is null
   */
  @Nullable
  public T setNew(T reference) {
    return reference != null && myHolder.compareAndSet(null, reference) ? reference : get();
  }

  /**
   * Changes state to disposed. Since the moment always return null and not allow to reinitialize.
   * @return reference for farther dispose or null if already disposed or never initialized.
   */
  @Nullable
  public T dispose() {
    while (true) {
      Object obj = myHolder.get();
      if (obj == DISPOSED) return null;
      if (myHolder.compareAndSet(obj, DISPOSED)) return (T)obj;
    }
  }
}
