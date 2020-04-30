package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.api.application.ItemOrder;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.enums.DefaultItemSelector;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.PlainTextCanvas;
import com.almworks.util.components.completion.CompletingComboBox;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SingleEditableEnumEditor extends BaseSingleEnumEditor  {
  private final ItemDisplayName myToString;
  private final ItemFromString myFromString;
  private final ItemRenderer myRenderer;

  public SingleEditableEnumEditor(NameMnemonic labelText, EnumVariantsSource variants, @Nullable DefaultItemSelector defaultItem, CanvasRenderable nullRenderable,
    @Nullable CanvasRenderer<ItemKey> defaultRenderer, boolean appendNull, EnumValueKey valueKey, boolean verify) {
    super(labelText, variants, defaultItem, appendNull, valueKey, verify);
    myRenderer = new ItemRenderer(nullRenderable, defaultRenderer);
    String text;
    if (nullRenderable != null) {
      PlainTextCanvas canvas = new PlainTextCanvas();
      nullRenderable.renderOn(canvas, CellState.LABEL);
      text = canvas.getText();
    } else text = "";
    myToString = new ItemDisplayName(text);
    myFromString = new ItemFromString(text);
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    CompletingComboBox<ItemKey> comboBox = new CompletingComboBox<ItemKey>();
    comboBox.setCasesensitive(true);
    ComponentControl.Enabled enableState = getComponentEnableState(model);
    comboBox.setPrototypeDisplayValue(new ItemKeyStub("sampleItemKey", "Sample Item Name", ItemOrder.NO_ORDER));
    ComponentControl control = AttachComboBox.attach(this, life, model, comboBox, myToString, myFromString, myRenderer, enableState, getDefaultItem());
    return control != null ? Collections.singletonList(control) : Collections.<ComponentControl>emptyList();
  }

  private static class ItemDisplayName extends Convertor<ItemKey, String> {
    private final String myNullText;

    private ItemDisplayName(String nullText) {
      myNullText = nullText;
    }

    @Override
    public String convert(ItemKey value) {
      if (value == null) return "";
      if (value == NULL_ITEM) {
        return myNullText;
      }
      return value.getDisplayName();
    }
  }

  private static class ItemFromString extends Convertor<String, ItemKey> {
    private final String myNullText;

    private ItemFromString(String nullText) {
      myNullText = nullText;
    }

    @Override
    public ItemKey convert(String value) {
      value = Util.NN(value).trim();
      if (value.length() == 0 || value.equals(myNullText)) return NULL_ITEM;
      return new ItemKeyStub(value, value, ItemOrder.byString(value));
    }
  }
}
