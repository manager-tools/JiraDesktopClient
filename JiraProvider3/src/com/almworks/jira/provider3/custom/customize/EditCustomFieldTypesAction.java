package com.almworks.jira.provider3.custom.customize;

import com.almworks.api.engine.Engine;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBPriority;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.custom.LoadAllFields;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.custom.impl.StoredCustomFieldConfig;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.*;
import org.almworks.util.TypedKey;

import java.util.List;
import java.util.Map;

public class EditCustomFieldTypesAction extends SimpleAction {
  public static final AnAction INSTANCE = new EditCustomFieldTypesAction();

  public EditCustomFieldTypesAction() {
    super("Edit Field Types\u2026");
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final CustomFieldsComponent customFields = context.getSourceObject(CustomFieldsComponent.ROLE);
    SyncManager syncManager = context.getSourceObject(SyncManager.ROLE);
    final DialogManager dialogManager = context.getSourceObject(DialogManager.ROLE);
    final LoadAllFields loadFields = new LoadAllFields();
    LoadAllFields.ConnectionName.addTo(loadFields, context.getSourceObject(Engine.ROLE).getConnectionManager());
    syncManager.enquireRead(DBPriority.FOREGROUND, new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        StoredCustomFieldConfig actual = StoredCustomFieldConfig.loadActual(reader);
        final List<Map<TypedKey<?>, ?>> current = actual != null ? actual.getConfigs() : customFields.loadDefaultFieldTypes();
        loadFields.load(reader);
        ThreadGate.AWT.execute(new Runnable() {
          @Override
          public void run() {
            DialogEditorBuilder builder = dialogManager.createEditor("editCustomFieldTypes");
            builder.setTitle("Jira Custom Field Types");
            CustomFieldTypesEditor.showEditor(builder, current, loadFields);
          }
        });
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }
    });
  }
}
