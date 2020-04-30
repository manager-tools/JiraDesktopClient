package com.almworks.util.ui;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ADateField;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.model.ValueModel;
import com.almworks.util.properties.BooleanPropertyKey;
import com.almworks.util.properties.PropertyKey;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.util.Date;
import java.util.List;

/**
 * @author dyoma
 */
public class ComponentKeyBinder {
  private final DetachComposite myDetach;
  private final ModelMapBinding myBinding;

  public ComponentKeyBinder(DetachComposite detach, ModelMapBinding binding) {
    myDetach = detach;
    myBinding = binding;
  }

  public void setDocument(@NotNull PropertyKey<Document, ?> key, @NotNull JTextComponent field) {
    myBinding.setDocument(myDetach, key, field);
  }

  public void setDate(@NotNull PropertyKey<ValueModel<Date>, ?> key, @NotNull ADateField field) {
    myBinding.setDate(myDetach, key, field);
  }

  public <T> void setCombobox(@NotNull PropertyKey<AComboboxModel<T>, ?> key, @NotNull AComboBox<T> comboBox) {
    myDetach.add(myBinding.setCombobox(key, comboBox));
  }

  public <T> void setMultipleSelection(@NotNull PropertyKey<OrderListModel<T>, List<T>> key, @NotNull SelectionAccessor<T> selection) {
    myDetach.add(myBinding.setMultipleSelection(key, selection));
  }

  public void setConditionalText(@NotNull PropertyKey.EnablingKey<Document, ?> key, @NotNull JToggleButton button, @NotNull JTextComponent field) {
    myDetach.add(myBinding.setConditionalText(key, button, field));
  }

  public void setBoolean(@NotNull BooleanPropertyKey key, @NotNull JToggleButton button) {
    myDetach.add(myBinding.setBoolean(key, button));
  }

  public void setInvertedBoolean(@NotNull BooleanPropertyKey key, @NotNull JToggleButton button) {
    myDetach.add(myBinding.setInvertedBoolean(key, button));
  }
}
