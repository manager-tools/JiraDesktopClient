package com.almworks.util.ui.actions;

import com.almworks.util.collections.Factories;
import com.almworks.util.commons.Factory;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.util.Map;


/**
 * @author : Dyoma
 * Replaced with {@link SimpleAction}
 */
@Deprecated
public abstract class AnAbstractAction implements AnAction {
  private final Map<TypedKey, Factory> myPropertyMap;

  protected AnAbstractAction(String text) {
    this(text, null);
  }

  protected AnAbstractAction(String text, Icon icon) {
    myPropertyMap = Collections15.hashMap();
    if (text != null)
      setDefaultPresentation(PresentationKey.NAME, text);
    if (icon != null)
      setDefaultPresentation(PresentationKey.SMALL_ICON, icon);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
  }

  public <T> void setDefaultPresentation(TypedKey<T> key, T value) {
    myPropertyMap.put(key, Factories.singleton(value));
  }

  public void update(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.ENABLED);
    for (Map.Entry<TypedKey,Factory> entry : myPropertyMap.entrySet()) {
      assert entry.getKey() instanceof PresentationKey<?> : entry;
      context.putPresentationProperty((PresentationKey<Object>) entry.getKey() , entry.getValue().create());
    }
  }

}
