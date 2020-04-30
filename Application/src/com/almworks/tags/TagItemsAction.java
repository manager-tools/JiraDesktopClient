package com.almworks.tags;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.tree.TagNode;
import com.almworks.api.gui.BasicWindowBuilder;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.util.Terms;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TagItemsAction extends SimpleAction {
  private TagItemsForm myForm;
  private static final String SELECTED_TAG = "selectedTag";

  public TagItemsAction() {
    super("Ta&gs\u2026", Icons.TAG_DEFAULT);
    setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  private static List<ItemWrapper> selectExisting(List<ItemWrapper> wrappers) {
    if (wrappers == null || wrappers.isEmpty()) return wrappers;
    List<ItemWrapper> copy = null;
    for (int i = 0; i < wrappers.size(); i++) {
      ItemWrapper wrapper = wrappers.get(i);
      if (wrapper.services().isRemoteDeleted()) {
        if (copy != null) continue;
        copy = Collections15.arrayList();
        if (i > 0) copy.addAll(wrappers.subList(0, i));
      } else if (copy != null) copy.add(wrapper);
    }
    return copy != null ? copy : wrappers;
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(false);
    List<ItemWrapper> wrappers = selectExisting(context.getSourceCollection(ItemWrapper.ITEM_WRAPPER));
    int count = wrappers.size();
    if (count == 0)
      throw new CantPerformException();
    if (count == 1)
      context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, "Change " + Terms.ref_artifact + " tags");
    else
      context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, "Tag/untag selected " + Terms.ref_artifacts);
    context.setEnabled(EnableState.ENABLED);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = selectExisting(
      CantPerformException.ensureNotEmpty(context.getSourceCollection(ItemWrapper.ITEM_WRAPPER)));
    List<? extends ItemKey> initialData = getSameTags(wrappers);
    createDialog(wrappers, initialData).showWindow();
  }

  private BasicWindowBuilder createDialog(final List<ItemWrapper> wrappers, List<? extends ItemKey> commonTags) {
    DialogManager dialogManager = Context.require(DialogManager.class);
    final DialogBuilder builder = dialogManager.createBuilder("tagItems");
    ExplorerComponent explorerComponent = Context.require(ExplorerComponent.class);
    Map<DBIdentifiedObject, TagNode> tagNodes = explorerComponent.getTags();

    builder.setTitle(Local.parse(createTitle(wrappers, commonTags)));
    builder.setModal(true);
    builder.setIgnoreStoredSize(true);
    final TagItemsForm form = getForm(commonTags, tagNodes.values());
    final Configuration config = builder.getConfiguration();
    form.selectTags(config.getAllSettings(SELECTED_TAG));
    builder.setContent(form);
    builder.setInitialFocusOwner(form.getInitiallyFocused());
    builder.setEmptyCancelAction();
    form.setAcceptAction(new Runnable() {
      public void run() {
        builder.pressOk();
      }
    });
    builder.setOkAction(new EnabledAction("OK") {
      protected void doPerform(ActionContext context) throws CantPerformException {
        config.setSettings(SELECTED_TAG, form.getSelectedIds());
        form.applyTo(wrappers, context);
      }
    });

    return builder;
  }

  private String createTitle(List<ItemWrapper> wrappers, List<? extends ItemKey> commonTags) {
    if (commonTags != null)
      if (wrappers.size() == 1)
        return "Tag " + Terms.ref_Artifact;
      else
        return "Tag Selected " + Terms.ref_Artifacts;
    else
      return "Change Tags for " + wrappers.size() + " Selected " + Terms.ref_Artifacts;
  }

  private TagItemsForm getForm(List<? extends ItemKey> commonTags, Collection<TagNode> tagNodes) {
    if (myForm == null)
      myForm = new TagItemsForm();
    myForm.attach(commonTags, tagNodes);
    return myForm;
  }

  @Nullable
  private List<? extends ItemKey> getSameTags(List<ItemWrapper> wrappers) {
    ItemWrapper wrapper = wrappers.get(0);
    List<? extends ItemKey> tags = getTags(wrapper);
    for (int i = 1; i < wrappers.size(); i++) {
      List<? extends ItemKey> otherTags = getTags(wrappers.get(i));
      if (!tags.equals(otherTags))
        return null;
    }
    return tags;
  }

  private List<? extends ItemKey> getTags(ItemWrapper wrapper) {
    ModelKey<List<ItemKey>> modelKey = TagsComponentImpl.getModelKey(wrapper.services().getActor(GuiFeaturesManager.ROLE));
    if (modelKey == null) return Collections.emptyList();
    List<ItemKey> data = wrapper.getModelKeyValue(modelKey);
    return data == null ? Collections15.<ItemKey>emptyList() : data;
  }
}
