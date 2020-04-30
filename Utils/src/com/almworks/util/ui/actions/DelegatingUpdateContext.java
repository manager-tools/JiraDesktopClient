package com.almworks.util.ui.actions;

import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.model.ScalarModel;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.text.Document;
import java.util.Map;

public abstract class DelegatingUpdateContext extends DelegatingActionContext implements UpdateContext {
  protected DelegatingUpdateContext(UpdateContext delegate) {
    super(delegate);
  }

  protected UpdateContext getDelegate() {
    return (UpdateContext) super.getDelegate();
  }

  public Map<PresentationKey<?>, Object> getAllValues() {
    return getDelegate().getAllValues();
  }

  public EnableState getEnabled() {
    return getDelegate().getEnabled();
  }

  public <T> T getPresentationProperty(PresentationKey<T> key) {
    return getDelegate().getPresentationProperty(key);
  }

  public boolean isDisabled() {
    return getDelegate().isDisabled();
  }

  public void addUpdateDetach(Detach detach) {
    getDelegate().addUpdateDetach(detach);
  }

  public Lifespan nextUpdateSpan() {
    return getDelegate().nextUpdateSpan();
  }

  public UpdateRequest getUpdateRequest() {
    return getDelegate().getUpdateRequest();
  }

  public <T> void putPresentationProperty(PresentationKey<T> key, T value) {
    getDelegate().putPresentationProperty(key, value);
  }

  public void setEnabled(EnableState enable) {
    getDelegate().setEnabled(enable);
  }

  public void setEnabled(boolean enabled) {
    getDelegate().setEnabled(enabled);
  }

  public void updateOnChange(Document document) {
    getDelegate().updateOnChange(document);
  }

  public void updateOnChange(CollectionModel<?> model) {
    getDelegate().updateOnChange(model);
  }

  public void updateOnChange(ScalarModel<?> model) {
    getDelegate().updateOnChange(model);
  }

  public void updateOnChange(SelectionInListModel<?> model) {
    getDelegate().updateOnChange(model);
  }

  public void updateOnChange(Modifiable modifiable) {
    getDelegate().updateOnChange(modifiable);
  }

  public void updateOnPropertyChange(JComponent component, String propertyName) {
    getDelegate().updateOnPropertyChange(component, propertyName);
  }

  public void watchModifiableRole(TypedKey<? extends Modifiable> role) {
    getDelegate().watchModifiableRole(role);
  }

  public void watchRole(TypedKey<?> role) {
    getDelegate().watchRole(role);
  }
}
