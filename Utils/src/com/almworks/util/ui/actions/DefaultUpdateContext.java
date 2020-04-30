package com.almworks.util.ui.actions;

import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.i18n.Local;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.model.ScalarModel;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class DefaultUpdateContext extends DefaultActionContext implements UpdateContext {
  private final UpdateRequest myUpdateRequest;
  private final Map<PresentationKey<?>, Object> myValues;

  public DefaultUpdateContext(Component component, Updatable updatable) {
    this(component, updatable, Collections15.<PresentationKey<?>, Object>hashMap());
  }

  private DefaultUpdateContext(Component context, Updatable updatable, Map<PresentationKey<?>, Object> values) {
    super(context);
    assert updatable != null;
    myUpdateRequest = new UpdateRequest(updatable, this);
    myValues = values;
  }

  public void setEnabled(EnableState enable) {
    putPresentationProperty(PresentationKey.ENABLE, enable);
  }

  public void setEnabled(boolean enabled) {
    setEnabled(enabled ? EnableState.ENABLED : EnableState.DISABLED);
  }

  public EnableState getEnabled() {
    return getPresentationProperty(PresentationKey.ENABLE);
  }

  public <T> T getPresentationProperty(PresentationKey<T> key) {
    return (T) myValues.get(key);
  }

  public UpdateRequest getUpdateRequest() {
    return myUpdateRequest;
  }

  public <T> void putPresentationProperty(PresentationKey<T> key, T value) {
    if (value instanceof String) {
      value = (T)Local.parse((String)value);
    }
    myValues.put(key, value);
  }

  /**
   * @return true iff action already recided to be {@link EnableState#DISABLED} or {@link EnableState#INVISIBLE}
   */
  public boolean isDisabled() {
    return myValues.get(PresentationKey.ENABLE) != EnableState.ENABLED;
  }

  public Map<PresentationKey<?> ,Object> getAllValues() {
    return myValues;
  }

  @NotNull
  public static UpdateContext singleUpdate(@NotNull JComponent context) {
    return new DefaultUpdateContext(context, Updatable.NEVER);
  }

  public void watchRole(TypedKey<?> role) {
    myUpdateRequest.watchRole(role);
  }

  public void updateOnChange(Document document) {
    myUpdateRequest.updateOnChange(document);
  }

  public void updateOnChange(Modifiable modifiable) {
    myUpdateRequest.updateOnChange(modifiable);
  }

  public void updateOnChange(SelectionInListModel<?> model) {
    myUpdateRequest.updateOnChange(model);
  }

  public void updateOnChange(CollectionModel<?> model) {
    myUpdateRequest.updateOnChange(model);
  }

  public void updateOnChange(ScalarModel<?> model) {
    myUpdateRequest.updateOnChange(model);
  }

  public void updateOnPropertyChange(JComponent component, String propertyName) {
    myUpdateRequest.updateOnPropertyChange(component, propertyName);
  }

  public void watchModifiableRole(TypedKey<? extends Modifiable> role) {
    myUpdateRequest.watchModifiableRole(role);
  }

  public void addUpdateDetach(Detach detach) {
    nextUpdateSpan().add(detach);
  }

  public Lifespan nextUpdateSpan() {
    return myUpdateRequest.getLifespan();
  }
}
