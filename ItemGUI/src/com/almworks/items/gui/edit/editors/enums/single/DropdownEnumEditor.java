package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.enums.DefaultItemSelector;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.recent.AddRecentFromComboBox;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class DropdownEnumEditor extends BaseSingleEnumEditor {

  private CanvasRenderable myNullRenderable;
  private CanvasRenderer<ItemKey> myRenderer;

  public DropdownEnumEditor(NameMnemonic labelText, EnumVariantsSource variants, @Nullable DefaultItemSelector defaultItem,
    CanvasRenderable nullRenderable, @Nullable CanvasRenderer<ItemKey> defaultRenderer, boolean appendNull, EnumValueKey valueKey, boolean verify) {
    super(labelText, variants, defaultItem, appendNull, valueKey, verify);
    myNullRenderable = nullRenderable;
    myRenderer = defaultRenderer;
  }
  
  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    return Collections.singletonList(createCombo(life, model));
  }

  public ComponentControl createCombo(Lifespan life, EditItemModel model) {
    return attachCombo(life, model, new AComboBox<ItemKey>(5));
  }

  public ComponentControl attachCombo(Lifespan life, EditItemModel model, AComboBox<ItemKey> comboBox) {
    Acceptor acceptor = new Acceptor(getEnumCanvasRenderer(model));
    getVariants().configure(life, model, acceptor);
    RecentController<ItemKey> recents = acceptor.setupComboBox(life, comboBox);
    connectCB(life, model, comboBox, recents);
    FieldEditorUtil.registerComponent(model, this, comboBox);
    return SimpleComponentControl.singleLine(comboBox, this, model, getComponentEnableState(model));
  }

  private CanvasRenderer<ItemKey> getEnumCanvasRenderer(EditItemModel model) {
    boolean allNotNull = isDifferentNotNullInitial(model);
    CanvasRenderable nullRenderable = allNotNull ? new CanvasRenderable.TextRenderable(Font.ITALIC, "<Keep current value>") : myNullRenderable;
    return new ItemRenderer(nullRenderable, myRenderer);
  }

  private static class Acceptor implements VariantsAcceptor<ItemKey> {
    private final RecentController<ItemKey> myRecents = new RecentController<ItemKey>();

    private Acceptor(CanvasRenderer<ItemKey> renderer) {
      myRecents.setIdentityConvertor(BaseSingleEnumEditor.RECENT_GET_ID);
      myRecents.setRenderer(renderer);
    }

    @Override
    public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
      myRecents.setup(variants, recentConfig);
    }

    public RecentController<ItemKey> setupComboBox(Lifespan life, AComboBox<ItemKey> comboBox) {
      myRecents.setupAComboBox(comboBox, life);
      AddRecentFromComboBox.install(life, myRecents, comboBox);
      life.add(myRecents.createDetach());
      return myRecents;
    }
  }
}
