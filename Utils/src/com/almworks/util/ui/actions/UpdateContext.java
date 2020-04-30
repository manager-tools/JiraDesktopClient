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

/**
 * @author dyoma
 */
public interface UpdateContext extends ActionContext {
  void setEnabled(EnableState enable);

  /**
   * identical to calling setEnabled(enabled ? EnableState.ENABLED : EnableState.DISABLED);
   */
  void setEnabled(boolean enabled);

  EnableState getEnabled();

  <T> T getPresentationProperty(PresentationKey<T> key);

  UpdateRequest getUpdateRequest();

  <T> void putPresentationProperty(PresentationKey<T> key, T value);

  boolean isDisabled();

  Map<PresentationKey<?> ,Object> getAllValues();

  void watchRole(TypedKey<?> role);

  void updateOnChange(Document document);

  void updateOnChange(Modifiable modifiable);

  void updateOnChange(CollectionModel<?> model);

  void updateOnChange(ScalarModel<?> model);

  void watchModifiableRole(TypedKey<? extends Modifiable> role);

  void addUpdateDetach(Detach detach);

  void updateOnChange(SelectionInListModel<?> model);

  Lifespan nextUpdateSpan();

  void updateOnPropertyChange(JComponent component, String propertyName);
}
