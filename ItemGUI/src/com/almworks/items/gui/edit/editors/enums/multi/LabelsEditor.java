package com.almworks.items.gui.edit.editors.enums.multi;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.UndoUtil;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

public class LabelsEditor extends BaseMultiEnumEditor {
  private final EnumItemCreator myCreator;

  public LabelsEditor(NameMnemonic labelText, DBAttribute<Set<Long>> attribute, EnumVariantsSource variants, EnumItemCreator creator) {
    super(labelText, attribute, variants);
    myCreator = creator;
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(final Lifespan life, EditItemModel model) {
    final JTextField field = new ATextField();
    field.setColumns(15);
    final FieldWithMoreButton<JTextField> fwmb = new FieldWithMoreButton<JTextField>();
    fwmb.setField(field);
    return Collections.singletonList(attachComponent(life, model, fwmb));
  }

  public ComponentControl attachComponent(final Lifespan life, final EditItemModel model, FieldWithMoreButton<JTextField> fwmb) {
    final JTextField field = fwmb.getField();
    final Object[] cell = { null };
    getVariants().configure(life, model, new VariantsAcceptor<ItemKey>() {
      @Override
      public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
        cell[0] = variants;
      }
    });
    final AListModel<ItemKey> variants = (AListModel<ItemKey>)cell[0];

    final Collection<? extends ItemKey> value = getValue(model);
    field.setText(toStringValue(value));

    final JointChangeListener listener = new JointChangeListener() {
      protected void processChange() {
        final Collection<? extends ItemKey> value = getValue(model);
        UIUtil.setTextKeepView(field, toStringValue(value));
        UndoUtil.discardUndo(field);
      }
    };
    model.addAWTChangeListener(life, listener);

    UIUtil.addTextListener(life, field, new JointChangeListener(listener.getUpdateFlag()) {
      protected void processChange() {
        setValue(model, toLabelsValue(field.getText(), variants));
      }
    });

    life.add(UIUtil.addFocusListener(field, new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        if(!life.isEnded()) {
          listener.onChange();
        }
      }
    }));

    final Lifecycle popCycle = new Lifecycle();
    life.add(popCycle.getDisposeDetach());

    fwmb.setAction(new LabelsDropDown(fwmb, model, variants, popCycle));
    FieldEditorUtil.registerComponent(model, this, field);
    return SimpleComponentControl.singleLine(fwmb, this, model, getComponentEnabledState(model));
  }

  private String toStringValue(Collection<? extends ItemKey> value) {
    if(value == null || value.isEmpty()) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    for(final ItemKey key : value) {
      sb.append(key.getDisplayName()).append(" ");
    }
    return sb.substring(0, sb.length() - 1);
  }

  private List<ItemKey> toLabelsValue(String text, AListModel<ItemKey> variants) {
    final Map<String, ItemKey> map = Collections15.hashMap();
    for(final ItemKey key : variants) {
      map.put(key.getId(), key);
    }

    final List<ItemKey> list = Collections15.arrayList();
    final Set<String> seen = Collections15.hashSet();
    for(final String label : TextUtil.getLabels(text)) {
      if(seen.add(label)) {
        ItemKey key = map.get(label);
        if(key == null) {
          key = new ItemKeyStub(label);
        }
        list.add(key);
      }
    }
    return list;
  }

  @Override
  protected LongList prepareCommitValue(CommitContext context) throws CancelCommitException {
    final LongArray items = new LongArray();
    for(final ItemKey key : getSelectedItemKeys(context.getModel())) {
      long item = key.getItem();
      if(item <= 0 && myCreator != null) item = myCreator.createItem(context, key.getId());
      if(item > 0) items.add(item);
      else LogHelper.error("Failed to create label", key);
    }
    return items;
  }

  private class LabelsDropDown extends DropDownListener.ForComponent implements ChangeListener {
    private final FieldWithMoreButton<JTextField> myComponent;
    private final EditModelState myModel;
    private final AListModel<ItemKey> myVariants;
    private final Lifecycle myPopLifecycle;

    private ACheckboxList<ItemKey> myList;

    public LabelsDropDown(
      FieldWithMoreButton<JTextField> component, EditModelState model,
      AListModel<ItemKey> variants, Lifecycle popLifecycle)
    {
      super(component);
      myComponent = component;
      myModel = model;
      myVariants = variants;
      myPopLifecycle = popLifecycle;
    }

    protected JComponent createPopupComponent() {
      final Collection<? extends ItemKey> value = getValue(myModel);
      final Set<ItemKey> variants = new TreeSet<ItemKey>();
      variants.addAll(myVariants.toList());
      variants.addAll(value);

      myList = new ACheckboxList<ItemKey>(FixedListModel.create(variants));

      final SelectionAccessor<ItemKey> checked = myList.getCheckedAccessor();
      checked.setSelected(value);

      final Lifespan life = myPopLifecycle.lifespan();
      checked.addAWTChangeListener(life, this);

      final JScrollPane scrollPane = new JScrollPane(myList);
      final Dimension d1 = myComponent.getSize();
      final Dimension d2 = UIUtil.getRelativeDimension(myComponent, 10, 10);
      scrollPane.setPreferredSize(new Dimension(d1.width, d2.height));

      final Color bg = ColorUtil.between(UIUtil.getEditorBackground(), Color.YELLOW, 0.1F);
      scrollPane.setBackground(bg);
      scrollPane.getViewport().setBackground(bg);
      myList.setBackground(bg);
      myList.getScrollable().setBackground(bg);

      scrollPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), HIDE_DROP_DOWN);

      Aqua.cleanScrollPaneBorder(scrollPane);

      return scrollPane;
    }

    public void onChange() {
      updateDocument(myComponent.getField().getDocument(), myList.getCheckedAccessor().getSelectedItems());
    }

    private void updateDocument(Document document, List<ItemKey> selectedItems) {
      final String oldText = Util.NN(DocumentUtil.getDocumentText(document)).trim();
      final List<String> labels = Collections15.arrayList(TextUtil.getLabels(oldText));
      final List<String> ids = ItemKey.GET_ID.collectList(selectedItems);
      labels.retainAll(ids);

      for(String id : ids) {
        id = id.trim();
        if(!id.isEmpty() && !labels.contains(id)) {
          labels.add(id);
        }
      }

      final String newText = TextUtil.separate(labels, " ");
      if(!newText.equalsIgnoreCase(oldText)) {
        DocumentUtil.setDocumentText(document, newText);
      }
    }

    protected void onDropDownHidden() {
      UIUtil.requestFocusLater(myComponent.getField());
      myPopLifecycle.cycle();
    }

    @Override
    protected void onDropDownShown() {
      final ACheckboxList<ItemKey> list = myList;
      if(list != null) {
        UIUtil.requestFocusLater(list.getSwingComponent());
        list.getSelectionAccessor().ensureSelectionExists();
      }
    }
  }
}
