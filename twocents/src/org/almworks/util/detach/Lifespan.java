package org.almworks.util.detach;

import org.jetbrains.annotations.Nullable;

/**
 * Lifespan is a period in the application's run time. At some point in time, lifespan ends.
 * <b>Lifespan owner</b> is someone who has a link to the implementation of lifespan. It is up to lifespan's owner
 * to decide when lifespan ends.
 * <b>Lifespan client</b> is someone who needs a lifespan. Usually it is someone who performs an "attach" operation and
 * needs to store "detach" operation somewhere.
 * <p>
 * This interface is used to ensure that object references such as listener lists get cleared when they are no longer
 * needed. A method that makes a subscription may require an instance of Lifespan to be passed as a parameter. This way
 * any programmer that is writing a method call gets the idea that a source-listener link is established and needs to
 * be cleared at some point in time.
 * <p>
 * After lifespan ends, any call to {@link #add} method will immediately call detach. It is advised to check that
 * the lifespan is not ended before adding, to produce less garbage. Example:
 * <pre>
 *   public void addListener(Lifespan lifespan, final Listener listener) {
 *     if (!lifespan.isEnded()) {
 *       myListeners.add(listener);
 *       lifespan.add(new Detach() {
 *         public void doDetach() {
 *           myListeners.remove(listener);
 *         }
 *       });
 *     }
 *   }
 * </pre>
 * <p>
 * <b>NB 1:</b> Lifespan's methods have no contractual obligations regarding threads.
 * It is up to lifespan owner and lifespan client to require or ensure thread safety.
 * <p>
 * <b>NB 2:</b> Parameter's {@link Detach#detach()} method may be called within {@link #add} method.
 */
public interface Lifespan {
  /**
   * {@link #FOREVER} is a special lifespan that never ends. Have a memory leak in your application? Search for usages
   * of {@link #FOREVER}.
   */
  Lifespan FOREVER = new Forever();

  /**
   * {@link #NEVER} is a special lifespan that has already ended. Any call to {@link #add} method will immediately call
   * parameter's detach.
   */
  Lifespan NEVER = new Never();

  /**
   * Adds detach to the lifespan. When lifespan ends, detach's {@link Detach#detach} method will be called. If the
   * lifespan has already ended, detach will be called immediately.  
   *
   * @param detach a procedure for clearing references to non-used objects. null is ignored.
   * @return this
   */
  Lifespan add(@Nullable Detach detach);

  /**
   * Checks if the lifespan has ended.
   *
   * @return true if the lifespan has ended
   */
  boolean isEnded();


  /**
   * Special singleton, see {@link Lifespan#FOREVER}.
   */
  final class Forever implements Lifespan {
    private Forever() {}

    public Lifespan add(Detach detach) {
      return this;
    }

    public boolean isEnded() {
      return false;
    }
  }


  /**
   * Special singleton, see {@link Lifespan#NEVER}.
   */
  final class Never implements Lifespan {
    private Never() {}

    public Lifespan add(Detach detach) {
      detach.detach();
      return this;
    }

    public boolean isEnded() {
      return true;
    }
  }
}
