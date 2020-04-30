package com.almworks.tags;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.tree.TagNode;
import com.almworks.explorer.tree.TagEditor;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.models.BaseTableColumnAccessor;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;

class TagItemsForm implements UIComponentWrapper {
  public final DataRole<TagOp> TAGOP_ROLE = DataRole.createRole(TagOp.class);
  private static final MyTagRenderer TAG_RENDERER = new MyTagRenderer();
  private static final TableColumnAccessor<TagOp, TagOp> TAG_COLUMN =
    BaseTableColumnAccessor.<TagOp>simple("Tag", Renderers.createRenderer(TAG_RENDERER));
  private static final TableColumnAccessor<TagOp, TagOp> CHECKBOX_COLUMN =
    TableColumnBuilder.<TagOp, TagOp>create("set", "Set")
      .setEditor(new CheckBoxTagOp())
      .setRenderer(new CheckBoxTagOp())
      .setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(5))
      .setConvertor(Convertor.<TagOp>identity())
      .createColumn();
  //    BaseTableColumnAccessor.simple("Set", new CheckBoxTagOp()).setDataEditor(new CheckBoxTagOp());
  private static final TableColumnAccessor<TagOp, TagOp> DONT_CHANGE_RADIO_COLUMN =
    createRadioColumn("Don't change", null);
  private static final TableColumnAccessor<TagOp, TagOp> SET_RADIO_COLUMN = createRadioColumn("Set", Boolean.TRUE);
  private static final TableColumnAccessor<TagOp, TagOp> CLEAR_RADIO_COLUMN = createRadioColumn("Clear", Boolean.FALSE);

  private final JPanel myWholePanel = new JPanel(new BorderLayout(0, 5));
  private final JButton myCreateButton = new JButton();
  private final ATable<TagOp> myTable = new ATable<TagOp>();
  private final OrderListModel<TagOp> myModel = OrderListModel.create();
  private final OrderListModel<TableColumnAccessor<TagOp, ?>> myColumnsModel = OrderListModel.create();

  private boolean myCheckBoxMode;
  private final MyEnterListener myEnterListener = new MyEnterListener();

  public TagItemsForm() {
    NameMnemonic.parseString("&New Tag").setToButton(myCreateButton);
    myCreateButton.setFocusable(false);
    myCreateButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        createNewTag();
      }
    });

    myWholePanel.add(myTable.wrapWithScrollPane(), BorderLayout.CENTER);
    ListSpeedSearch.install(myTable, 0, TAG_RENDERER).setIgnoreSpace(true);
    myTable.setCollectionModel(myModel);
    myTable.setColumnModel(myColumnsModel);
    myTable.setGridHidden();
    myTable.setDataRoles(TAGOP_ROLE);
    myTable.setStriped(true);
    myTable.getSwingComponent().addKeyListener(myEnterListener);

    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
    toolbar.add(myCreateButton);

    AActionButton editButton = new AActionButton();
    editButton.setFocusable(false);
    editButton.setContextComponent(myTable);
    editButton.setAnAction(new SimpleAction("&Edit Tag") {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.setEnabled(false);
        context.watchRole(TAGOP_ROLE);
        TagOp op = context.getSourceObject(TAGOP_ROLE);
        TagNode node = op.getNode();
        if(node != null && node.isNode()) {
          if(!node.isEditable()) {
            throw new CantPerformException("Expected editable node.");
          }
        }
        context.setEnabled(true);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        TagOp op = context.getSourceObject(TAGOP_ROLE);
        TagNode node = op.getNode();
        if (node != null && node.isNode()) {
          boolean ok = node.editNode(context);
          if (ok) {
            myModel.updateElement(op);
          }
        }
      }
    });
    toolbar.add(editButton);
    toolbar.setAlignmentX(0F);
    myWholePanel.add(toolbar, BorderLayout.SOUTH);

    myTable.getSwingComponent().addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          toggleSelection();
        }
      }
    });
  }

  private void toggleSelection() {
    int[] selection = myTable.getSelectionAccessor().getSelectedIndexes();
    if (selection.length == 0) return;
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    Boolean value = null;
    for (int i = 0; i < selection.length; i++) {
      int opIndex = selection[i];
      TagOp op = myModel.getAt(opIndex);
      if (i == 0) {
        op.toggle(myCheckBoxMode);
        value = op.getChange();
      } else {
        op.setChange(value);
        op.setChanged(true);
      }
      min = Math.min(min, opIndex);
      max = Math.max(max, opIndex);
    }
    if(selection.length > 0 && myModel.getSize() > 0) {
      myModel.updateRange(Math.max(0, min), Math.min(max, myModel.getSize() - 1));
    }
  }

  private void createNewTag() {
    try {
      TagNode node = TagEditor.editAndCreateNode(new DefaultActionContext(myCreateButton));
      if (node != null) {
        TagOp op = new TagOp(node);
        op.setChange(Boolean.TRUE);
        op.setChanged(true);
        myModel.addElement(op);
        myTable.getSelectionAccessor().setSelected(op);
        myTable.scrollSelectionToView();
      }
    } catch (CantPerformException e) {
      Log.warn("cannot create tag", e);
    }
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void dispose() {
    myModel.clear();
  }

  public void attach(List<? extends ItemKey> commonTags, Collection<TagNode> tagNodes) {
    myCheckBoxMode = commonTags != null;

    myModel.clear();
    for (TagNode tagNode : tagNodes) {
      TagOp op = new TagOp(tagNode);
      if (commonTags != null) {
        op.setChange(isSet(commonTags, tagNode));
      }
      myModel.addElement(op);
    }

    myColumnsModel.clear();
    myColumnsModel.addElement(TAG_COLUMN);

    if (myCheckBoxMode) {
      myColumnsModel.addElement(CHECKBOX_COLUMN);
    } else {
      myColumnsModel.addElement(DONT_CHANGE_RADIO_COLUMN);
      myColumnsModel.addElement(SET_RADIO_COLUMN);
      myColumnsModel.addElement(CLEAR_RADIO_COLUMN);
    }

    if (myModel.getSize() > 0) {
      TagOp v = myModel.getAt(0);
      TableColumnAccessor<TagOp, ?> secondColumn = myColumnsModel.getAt(1);
      JComponent c = secondColumn.getDataRenderer().getRendererComponent(CellState.LABEL, v);
      if (c != null) {
        Dimension psize = c.getPreferredSize();
        if (psize != null) {
          myTable.setRowHeight(psize.height);
        }
      }
    }
    myTable.getSwingComponent().doLayout();
  }

  private static TableColumnAccessor<TagOp, TagOp> createRadioColumn(String name, Boolean state) {
    return TableColumnBuilder.<TagOp, TagOp>create(name, name)
      .setEditor(new RadioTagOp(state))
      .setRenderer(new RadioTagOp(state))
      .setSizePolicy(ColumnSizePolicy.Calculated.fixedTextWithMargin(name, 2))
      .setConvertor(Convertor.<TagOp>identity())
      .createColumn();
  }

  private boolean isSet(List<? extends ItemKey> commonTags, TagNode tagNode) {
    if (commonTags.isEmpty())
      return false;
    long item = tagNode.getTagItem();
    if (item == 0) return false;
    for (ItemKey commonTag : commonTags) {
      if (item == commonTag.getResolvedItem())
        return true;
    }
    return false;
  }

  public void applyTo(final Collection<ItemWrapper> items, ActionContext context) throws CantPerformException {
    AggregatingEditCommit commit = new AggregatingEditCommit();
    final List<TagOp> ops = Collections15.arrayList(myModel.toList());
    for (TagOp op : ops) {
      Boolean action = op.getChange();
      TagNode node = op.getNode();
      if (op.isChanged() && action != null && node.isNode()) {
        node.setOrClearTag(items, action, commit);
      }
    }
    context.getSourceObject(SyncManager.ROLE).commitEdit(commit);
  }

  public void setAcceptAction(Runnable acceptAction) {
    myEnterListener.setAcceptAction(acceptAction);
  }

  public Component getInitiallyFocused() {
    return myTable.getSwingComponent();
  }

  public void selectTags(Collection<String> tagIds) {
    SelectionAccessor<TagOp> selection = myTable.getSelectionAccessor();
    if (!tagIds.isEmpty()) {
      AListModel<? extends TagOp> model = myTable.getCollectionModel();
      for (int i = 0; i < model.getSize(); i++) {
        TagOp tag = model.getAt(i);
        if (tagIds.contains(tag.getId())) selection.addSelectionIndex(i);
      }
    }
    selection.ensureSelectionExists();
  }

  public List<String> getSelectedIds() {
    List<String> ids = Collections15.arrayList();
    List<TagOp> tags = myTable.getSelectionAccessor().getSelectedItems();
    for (TagOp tag : tags) ids.add(tag.getId());
    return ids;
  }


  private static class TagOp {
    private final TagNode myTagNode;

    /**
     * Action to be performed: null -- don't change, true -- set, false -- clear
     */
    private Boolean myChange;

    /**
     * If true, then myChange value has been changed by the user
     */
    private boolean myChanged;

    public TagOp(TagNode tagNode) {
      myTagNode = tagNode;
    }

    public void setChange(Boolean change) {
      myChange = change;
    }

    public void setChanged(boolean changed) {
      myChanged = changed;
    }

    public TagNode getNode() {
      return myTagNode;
    }

    public Boolean getChange() {
      return myChange;
    }

    public boolean isChanged() {
      return myChanged;
    }

    public String toString() {
      return "TO[" + myTagNode + "," + myChange + "," + myChanged + "]";
    }

    public String getId() {
      return myTagNode.getNodeId();
    }

    public void toggle(boolean checkboxMode) {
      if (checkboxMode) {
        myChange = !(myChange != null && myChange);
      } else {
        if (myChange == null)
          myChange = true;
        else if (myChange)
          myChange = false;
        else
          myChange = null;
      }
      myChanged = true;
    }
  }

  private static class CheckBoxTagOp extends ButtonActor.Checkbox<TagOp> {
    protected boolean isSelected(TagOp item) {
      Boolean b = item.getChange();
      assert b != null : item;
      return b != null && b;
    }

    protected void act(TagOp op) {
      op.toggle(true);
    }
  }

  private static class RadioTagOp extends ButtonActor.Radio<TagOp> {
    private final Boolean myState;

    public RadioTagOp(Boolean state) {
      super();
      myState = state;
    }

    protected void act(TagOp op) {
      op.setChange(myState);
      op.setChanged(true);
    }

    protected boolean isSelected(TagOp item) {
      return Util.equals(myState, item.getChange());
    }
  }

  private static class MyTagRenderer implements CanvasRenderer<TagOp> {
    public void renderStateOn(CellState state, Canvas canvas, TagOp item) {
      TagNode node = item.getNode();
      canvas.setIcon(node.getPresentation().getOpenIcon());
      canvas.appendText(node.getName());
    }
  }

  private static class MyEnterListener extends KeyAdapter {
    private Runnable myAcceptAction;

    public void setAcceptAction(@Nullable Runnable acceptAction) {
      myAcceptAction = acceptAction;
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        if (myAcceptAction != null) {
          myAcceptAction.run();
        }
        e.consume();
      }
    }
  }
}
