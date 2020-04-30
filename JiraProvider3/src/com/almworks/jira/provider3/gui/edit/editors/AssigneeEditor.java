package com.almworks.jira.provider3.gui.edit.editors;

import com.almworks.api.application.ItemKey;
import com.almworks.api.engine.Connection;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.composition.DelegatingFieldEditor;
import com.almworks.items.gui.edit.editors.enums.*;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.items.gui.edit.editors.enums.single.SingleEditableEnumEditor;
import com.almworks.items.gui.edit.editors.enums.single.SingleEnumDefaultValue;
import com.almworks.items.gui.edit.editors.enums.single.SingleEnumWithInlineButtonEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.jira.provider3.schema.User;
import com.almworks.jira.provider3.users.LoadAssignableUsers;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Condition;
import com.almworks.util.images.Icons;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class AssigneeEditor implements SingleEnumDefaultValue {
  private static final DelegatingFieldEditor<SingleEnumWithInlineButtonEditor<SingleEditableEnumEditor>> WRAPPER;
  public static final SingleEnumWithInlineButtonEditor<SingleEditableEnumEditor> MERGE_EDITOR;
  private static final AssigneeVariants ASSIGNABLE_USERS = new AssigneeVariants();
  private static final String ASSIGN_FILTER_ON = "assignee.filter";

  static {
    final SingleEnumWithInlineButtonEditor<SingleEditableEnumEditor> create = createEditor("<Default Assignee>");
    final SingleEnumWithInlineButtonEditor<SingleEditableEnumEditor> edit = createEditor("<Unassigned>");
    WRAPPER = new DelegatingFieldEditor<SingleEnumWithInlineButtonEditor<SingleEditableEnumEditor>>() {
      @Override
      protected SingleEnumWithInlineButtonEditor<SingleEditableEnumEditor> getDelegate(VersionSource source, EditModelState model) {
        return FieldEditorUtil.isNewItem(source, model) ? create : edit;
      }
    };
    MERGE_EDITOR = createEditor("<Unassigned>", ConnectionVariants.createStatic(User.ENUM_TYPE, MetaSchema.CONFIG_ASSIGNEE));
  }

  public static final FieldEditor EDITOR = WRAPPER;

  private static SingleEnumWithInlineButtonEditor<SingleEditableEnumEditor> createEditor(String nullPresentation) {
    return createEditor(nullPresentation, ASSIGNABLE_USERS);
  }

  private static SingleEnumWithInlineButtonEditor<SingleEditableEnumEditor> createEditor(String nullPresentation, EnumVariantsSource variants) {
    SingleEditableEnumEditor baseEditor = new DropdownEditorBuilder().setVariants(variants)
      .setAttribute(Issue.ASSIGNEE, User.CREATOR)
      .setNullPresentation(nullPresentation)
      .setLabelText(NameMnemonic.parseString("&Assignee"))
      .overrideRenderer(ItemKey.NAME_ID_RENDERER)
      .createEditable();
    SingleEnumWithInlineButtonEditor<SingleEditableEnumEditor> editor =
      SingleEnumWithInlineButtonEditor.create(baseEditor, new AssigneeEditor());
    editor.setActionName("Assign to me");
    editor.setIcon(Icons.ACTION_SET_USER_ME);
    editor.setActionDelegate(JiraActions.ASSIGN_TO_ME);
    return editor;
  }

  private final TypedKey<Long> myThisUser = TypedKey.create("assignee/thisUser");

  private AssigneeEditor() {
  }

  @Override
  public void prepare(VersionSource source, EditItemModel model) {
    JiraConnection3 connection = EngineConsts.getConnection(JiraConnection3.class, model);
    if (connection == null) return;
    long connectionItem = connection.getConnectionItem();
    if (connectionItem <= 0) return;
    long thisUser = source.forItem(connectionItem).getNNValue(Connection.USER, 0l);
    if (thisUser <= 0) return;
    model.putHint(myThisUser, thisUser);
    EngineConsts.ensureGuiFeatureManager(source, model);
  }

  @Override
  public boolean isEnabled(EditItemModel model) {
    return getValue(model) != null;
  }

  @Override
  public Pair<? extends ItemKey, Long> getValue(EditItemModel model) {
    return SingleEnumWithInlineButtonEditor.getItemValue(model, myThisUser, User.ENUM_TYPE);
  }

  private static class AssigneeVariants extends EnumTypeProvider.Source {
    private static final EnumTypeProvider.StaticEnum ENUM = new EnumTypeProvider.StaticEnum(User.ENUM_TYPE, "assignee");
    private final TypedKey<Map<Long, LongArray>> myAssignable = TypedKey.create("assignableUsers");

    private AssigneeVariants() {
      super(ENUM);
    }

    @Override
    public void prepare(VersionSource source, EditModelState model) {
      super.prepare(source, model);
      Connection connection = model.getValue(EngineConsts.VALUE_CONNECTION);
      if (connection == null) {
        LogHelper.error("Missing connection");
        return;
      }
      Map<Long, LongArray> assignable = Collections15.hashMap();
      LongArray projects = source.getReader().query(DPEqualsIdentified.create(DBAttribute.TYPE, Project.DB_TYPE)).copyItemsSorted();
      for (ItemVersion project : source.readItems(projects)) {
        LongArray users;
        if (Env.getBoolean(ASSIGN_FILTER_ON, true)) {
          users = LongArray.create(project.getValue(LoadAssignableUsers.A_ASSIGNABLE_USERS));
          if (users.isEmpty()) users = null;
          else users.sortUnique();
        } else users = null;
        assignable.put(project.getItem(), users);
      }
      model.putHint(myAssignable, assignable);
    }

    @Override
    public void configure(Lifespan life, EditItemModel model, VariantsAcceptor<ItemKey> acceptor) {
      new EnumModelConfigurator(model, acceptor){
        @Override
        protected void collectCubeAttributes(HashSet<DBAttribute<Long>> attributes) {
          attributes.add(Issue.PROJECT);
        }

        @Override
        protected void updateVariants(Lifespan life, VariantsAcceptor<ItemKey> acceptor, AListModel<LoadedItemKey> variants, EditItemModel model, UserDataHolder data) {
          ConnectionVariants.sendToAcceptor(acceptor, variants, model, MetaSchema.CONFIG_ASSIGNEE);
        }

        @Override
        protected AListModel<LoadedItemKey> getSortedVariantsModel(Lifespan life, EditItemModel model, ItemHypercube cube) {
          AListModel<LoadedItemKey> users = getValueModel(life, model, cube);
          FilteringListDecorator<LoadedItemKey> unsortedModel = FilteringListDecorator.create(life, users, getUserFilter(model, cube));
          return SortedListDecorator.create(life, unsortedModel, ItemKey.COMPARATOR);
        }
      }.start(life);
    }

    @Nullable
    private Condition<? super LoadedItemKey> getUserFilter(EditItemModel model, ItemHypercube cube) {
      final Map<Long, LongArray> map = model.getValue(myAssignable);
      if (map == null) return null;
      final Collection<Long> projects = Util.NN(cube.getIncludedValues(Issue.PROJECT), Collections.<Long>emptySet());
      return new Condition<LoadedItemKey>() {
        @Override
        public boolean isAccepted(LoadedItemKey value) {
          for (Long project : projects) {
            LongArray users = map.get(project);
            if (users == null) return true; // No assignable users for project
            if (users.binarySearch(value.getItem()) >= 0) return true; // Assignable
          }
          return false;
        }
      };
    }
  }
}
