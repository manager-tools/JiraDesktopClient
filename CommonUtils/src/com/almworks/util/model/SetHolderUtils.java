package com.almworks.util.model;

import com.almworks.util.collections.ChangeListener;
import org.jetbrains.annotations.NotNull;

public class SetHolderUtils {
  /**
   * Use only for sequential listeners that are subscribed to SetHolder with {@link com.almworks.util.exec.ThreadGate#AWT AWT} or {@link com.almworks.util.exec.ThreadGate#LONG_QUEUED(Object) long keyed} gates.
   * */
  public static <T> SetHolder.Listener<T> actualizingSequentialListener(final SetHolder.Listener<T> listener) {
    return new SetHolder.Listener<T>() {
      private long myVersion;
      @Override
      public void onSetChanged(@NotNull SetHolder.Event<T> evt) {
        myVersion = evt.actualize(myVersion);
        if (!evt.isEmpty()) {
          listener.onSetChanged(evt);
        }
      }
    };
  }

  public static SetHolder.Listener fromChangeListener(final ChangeListener changeListener) {
    return new SetHolder.Listener() {
      @Override
      public void onSetChanged(@NotNull SetHolder.Event event) {
        changeListener.onChange();
      }
    };
  }
}
