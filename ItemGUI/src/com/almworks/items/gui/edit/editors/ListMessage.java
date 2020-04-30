package com.almworks.items.gui.edit.editors;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.components.AList;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class ListMessage<T> extends MockEditor {
  private final List<T> myMessage;
  private CanvasRenderer<? super T> myRenderer = Renderers.defaultCanvasRenderer();

  public ListMessage(NameMnemonic labelText, List<T> message) {
    super(labelText);
    myMessage = message;
  }

  public static <T> ListMessage<T> create(NameMnemonic labelText, List<T> message) {
    return new ListMessage<T>(labelText, message);
  }

  public void setRenderer(CanvasRenderer<? super T> renderer) {
    myRenderer = renderer;
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    model.registerEditor(this);
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    AList<T> list = AList.create();
    list.setEnabled(false);
    list.setCollectionModel(FixedListModel.create(myMessage));
    list.setCanvasRenderer(myRenderer);
    return SimpleComponentControl.singleComponent(new JScrollPane(list), ComponentControl.Dimensions.WIDE, this, model, ComponentControl.Enabled.NOT_APPLICABLE);
  }
}
