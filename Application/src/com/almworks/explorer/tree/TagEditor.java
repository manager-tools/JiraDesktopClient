package com.almworks.explorer.tree;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.application.tree.TagNode;
import com.almworks.api.application.tree.TreeNodeFactory;
import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.gui.DialogResult;
import com.almworks.api.misc.FileCollectionBasedIcon;
import com.almworks.api.misc.WorkArea;
import com.almworks.tags.TagIcons;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.exec.Context;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.PresentationMapping;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.Collection;

public class TagEditor {
  private static final long MINIMUM_RESCAN_PERIOD = 120000;
  private static final IconData DEFAULT_ICON = new IconData("default icon", Icons.TAG_DEFAULT, "");
  private static final IconData NO_ICON = new IconData("no icon", PresentationMapping.EMPTY_ICON, TagIcons.NO_ICON);

  private static TagEditor ourInstance;

  private boolean myLocked;
  private long myLastRescanTime;
  private int myLastRescanFocusCount = -1;

  private JPanel myWholePanel;
  private JTextField myTagName;
  private AComboBox<IconData> myTagIcon;

  private final OrderListModel<IconData> myModel = OrderListModel.create();
  private final SelectionInListModel<IconData> myComboModel =
    SelectionInListModel.create(Lifespan.FOREVER, myModel, null);

  public TagEditor() {
    myTagIcon.setModel(myComboModel);
    myTagIcon.setCanvasRenderer(Renderers.defaultCanvasRenderer());
    UIUtil.setDefaultLabelAlignment(myWholePanel);
    Aqua.disableMnemonics(myWholePanel);
  }

  public static synchronized TagEditor instance() {
    TagEditor instance = ourInstance;
    if (instance == null) {
      ourInstance = instance = new TagEditor();
    }
    if (!instance.lockUse()) {
      return new TagEditor();
    }
    return instance;
  }

  private boolean lockUse() {
    if (myLocked)
      return false;
    myLocked = true;
    return true;
  }

  public void dispose() {
    assert myLocked : this;
    myLocked = false;
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void applyTo(TagNodeImpl tagNode) {
    String name = myTagName.getText().trim();
    IconData selected = myComboModel.getSelectedItem();
    tagNode.updatePresentation(name, selected == null ? null : selected.getIconPath());
  }

  public void resetTo(TagNodeImpl tagNode) {
    resetTo(tagNode.getName(), tagNode.getIconPath());
  }

  private void resetTo(String name, String iconPath) {
    maybeRescanIcons();
    UIUtil.setFieldText(myTagName, name);
    UIUtil.scrollToTop(myTagName);
    myTagName.selectAll();
    myComboModel.setSelectedItem(findIconData(iconPath));
  }

  @NotNull
  private IconData findIconData(String iconPath) {
    int count = myModel.getSize();
    for (int i = 0; i < count; i++) {
      IconData data = myModel.getAt(i);
      if (Util.equals(iconPath, data.getIconPath())) {
        return data;
      }
    }
    return NO_ICON;
  }

  private void maybeRescanIcons() {
    ApplicationManager manager = Context.get(ApplicationManager.class);
    int count = manager == null ? 0 : manager.getFocusSwitchCount();
    long now = System.currentTimeMillis();
    if (myLastRescanTime < now - MINIMUM_RESCAN_PERIOD || count > myLastRescanFocusCount) {
      rescan();
      myLastRescanTime = now;
      myLastRescanFocusCount = count;
    }
  }

  private void rescan() {
    myModel.clear();
    myModel.addElement(DEFAULT_ICON);
    myModel.addElement(NO_ICON);
    WorkArea workArea = Context.get(WorkArea.class);
    if (workArea != null) {
      Collection<File> files = workArea.getEtcCollectionFiles(TagIcons.TAG_ICONS_COLLECTION);
      if (files != null) {
        for (File file : files) {
          String name = file.getName();
          if (isIconFile(name)) {
            Icon icon = new FileCollectionBasedIcon(TagIcons.TAG_ICONS_COLLECTION, name, true);
            myModel.addElement(new IconData(stripExtension(name), icon, name));
          }
        }
      }
    }
  }

  private String stripExtension(String name) {
    int k = name.lastIndexOf('.');
    return k > 0 ? name.substring(0, k) : name;
  }

  private boolean isIconFile(String name) {
    return Util.lower(name).endsWith(".png");
  }

  public static boolean editNode(TagNodeImpl tagNode, ActionContext context) throws CantPerformException {
    DialogResult result = DialogResult.create(context, "editTag");
    result.pack();
    result.setOkResult("ok").setCancelResult("cancel").setBottomBevel(false);
    TagEditor editor = TagEditor.instance();
    editor.resetTo(tagNode);
    result.setInitialFocusOwner(editor.myTagName);
    Object r = result.showModal("Edit Tag", editor.getComponent());
    boolean ok = "ok".equals(r);
    if (ok) {
      editor.applyTo(tagNode);
    }
    editor.dispose();
    return ok;
  }

  public static TagNode editAndCreateNode(ActionContext context)
    throws CantPerformException
  {
    DialogResult result = DialogResult.create(context, "editTag");
    result.pack();
    result.setOkResult("ok").setCancelResult("cancel").setBottomBevel(false);
    TagEditor editor = instance();
    editor.resetTo("New Tag", "");
    result.setInitialFocusOwner(editor.myTagName);
    Object r = result.showModal("New Tag", editor.getComponent());
    boolean ok = "ok".equals(r);
    TagNode tagNode = null;
    if (ok) {
      ExplorerComponent explorerComponent = Context.require(ExplorerComponent.class);
      RootNode rootNode = explorerComponent.getRootNode();
      assert rootNode != null;
      TreeNodeFactory nodeFactory = rootNode.getNodeFactory();
      tagNode = nodeFactory.createTag(getTagsFolder(rootNode));
      editor.applyTo((TagNodeImpl) tagNode);
      nodeFactory.selectNode(tagNode, true);
    }
    editor.dispose();
    return tagNode;
  }

  @ThreadAWT
  public static TagsFolderNode getTagsFolder(@NotNull RootNode rootNode) {
    for (int i = 0; i < rootNode.getChildrenCount(); i++) {
      GenericNode child = rootNode.getChildAt(i);
      if (child instanceof TagsFolderNode) {
        return (TagsFolderNode) child;
      }
    }
    return null;
  }

  public static void editTag(TagNode tag, String name, String iconPath) {
    if (tag instanceof TagNodeImpl) {
      ((TagNodeImpl)tag).updatePresentation(name, iconPath);
    } else assert false : tag;
  }

  private static class IconData implements CanvasRenderable {
    private final String myText;
    private final Icon myIcon;
    private final String myIconPath;

    public IconData(String text, Icon icon, String iconPath) {
      myText = text;
      myIcon = icon;
      myIconPath = iconPath;
    }

    public void renderOn(Canvas canvas, CellState state) {
      if (myIcon != null)
        canvas.setIcon(myIcon);
      if (myText != null)
        canvas.appendText(myText);
    }

    public String getIconPath() {
      return myIconPath;
    }
  }
}
