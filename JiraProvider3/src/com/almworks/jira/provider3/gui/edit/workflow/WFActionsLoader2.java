package com.almworks.jira.provider3.gui.edit.workflow;

import com.almworks.actions.console.actionsource.ActionGroup;
import com.almworks.actions.console.actionsource.ConsoleActionsComponent;
import com.almworks.api.application.ItemWrapper;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.QueryImageSlice;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.cache.util.CachedItem;
import com.almworks.items.cache.util.CachedItemImpl;
import com.almworks.items.cache.util.ItemSetModel;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.gui.edit.workflow.duplicate.ResolveAsDuplicateSupport;
import com.almworks.jira.provider3.schema.WorkflowAction;
import com.almworks.util.advmodel.*;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class WFActionsLoader2 {
  private static final AttributeLoader<Long> CONNECTION = AttributeLoader.create(SyncAttributes.CONNECTION);
  private final ItemSetModel<CachedItem> myModel;
  private final SegmentedListModel<IdentifiableAction> myActions = SegmentedListModel.create(AListModel.EMPTY);

  public WFActionsLoader2(DBImage image) {
    QueryImageSlice slice = image.createQuerySlice(DPEqualsIdentified.create(DBAttribute.TYPE, WorkflowAction.DB_TYPE));
    slice.addAttributes(WorkflowAction.ID, WorkflowAction.PROJECT, WorkflowAction.ISSUE_TYPE);
    slice.addData(CONNECTION, WorkflowAction.NAME, WorkflowAction.FIELDS, WorkflowAction.TARGET_STATUS);
    myModel = CachedItemImpl.notStartedModel(slice);
  }

  public void start(final Lifespan life) {
    ThreadGate.AWT.execute(new Runnable() {
      @Override
      public void run() {
        myModel.getSlice().ensureStarted(life);
        myModel.start(life);
        AListModel<String> actionNames = ConvertingListDecorator.create(myModel, CachedItem.ValueGetter.create(WorkflowAction.NAME));
        FilteringListDecorator<String> nnNames = FilteringListDecorator.create(life, actionNames, Condition.<String>notNull());
        AListModel<String> sortedNames = SortedListDecorator.create(life, nnNames, String.CASE_INSENSITIVE_ORDER);
        AListModel<String> uniqueNames = UniqueListDecorator.create(life, sortedNames);
        AListModel<IdentifiableAction> actions = ActionNameWrapper.createModel(life, uniqueNames);
        myActions.setSegment(0, actions);
        life.add(new Detach() {
          @Override
          protected void doDetach() throws Exception {
            myActions.setSegment(0, AListModel.EMPTY);
          }
        });
      }
    });
  }

  public void registerConsoleActions(ConsoleActionsComponent consoleActions) {
    consoleActions.addGroup(new ActionGroup() {
      @Override
      public void update(UpdateContext context) throws CantPerformException {
        context.putPresentationProperty(PresentationKey.ENABLE, EnableState.ENABLED);
        context.putPresentationProperty(PresentationKey.NAME, "Workflow");
        InContext.checkContext(context, ItemWrapper.ITEM_WRAPPER);
        ArrayList<AnAction> actions = Collections15.<AnAction>arrayList(getModel().toList());
        actions.add(ResolveAsDuplicateSupport.ACTION);
        context.putPresentationProperty(ACTIONS, actions);
      }
    });
  }

  public AListModel<? extends IdentifiableAction> getModel() {
    return myActions;
  }

  public Modifiable getModifiable() {
    return myModel;
  }

  public List<CachedItem> getActions(long connection, String name, @Nullable LongList fields) {
    if (name == null || connection <= 0) return Collections15.emptyList();
    ArrayList<CachedItem> result = Collections15.arrayList();
    ImageSlice slice = myModel.getSlice();
    int index = 0;
    while ((index = slice.findIndexByValue(index, CONNECTION, connection)) >= 0) {
      long item = slice.getItem(index);
      if (name.equals(slice.getValue(item, WorkflowAction.NAME))
        && (fields == null || fields.equals(slice.getValue(item, WorkflowAction.FIELDS))))
        result.add(new CachedItemImpl(slice.getImage(), item));
      index++;
    }
    return result;
  }

//  /**
//   * Collects all known edit features with specified name
//   * @param name workflow action name
//   * @return not null. Not empty result is ArrayList instance.
//   */
//  @NotNull
//  public List<WorkflowEditFeature> getFeaturesByName(String name) {
//    ImageSlice slice = myModel.getSlice();
//    IntList indexes = slice.selectIndexesByValue(WorkflowAction.NAME, name);
//    if (indexes.isEmpty()) return Collections.emptyList();
//    ArrayList<WorkflowEditFeature> actions = Collections15.arrayList();
//    for (IntIterator cur : indexes) {
//      long item = slice.getItem(cur.value());
//      long connection = slice.getNNValue(item, CONNECTION, 0l);
//      if (connection <= 0) continue;
//      LongList fields = slice.getNNValue(item, WorkflowAction.FIELDS, LongList.EMPTY);
//      actions.add(new WorkflowEditFeature(connection, name, fields));
//    }
//    return actions;
//  }

  public Collection<String> getActioNames() {
    HashSet<String> names = Collections15.hashSet();
    ImageSlice slice = myModel.getSlice();
    for (int i = 0; i < slice.getActualCount(); i++) {
      long item = slice.getItem(i);
      String name = slice.getValue(item, WorkflowAction.NAME);
      if (name != null && name.length() > 0) names.add(name);
    }
    return names;
  }
}
