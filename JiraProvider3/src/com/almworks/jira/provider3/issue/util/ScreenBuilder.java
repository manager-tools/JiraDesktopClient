package com.almworks.jira.provider3.issue.util;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.gui.edit.EditorsScheme;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.issue.editor.IssueScreen;
import com.almworks.jira.provider3.issue.editor.ScreenController;
import com.almworks.jira.provider3.issue.editor.ScreenSet;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.LogHelper;
import com.almworks.util.config.Configuration;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScreenBuilder {
  private final List<String> myFields = Collections15.arrayList();
  private final List<FieldEditor> myEditors = Collections15.arrayList();
  private final EditorsScheme myScheme;
  private int myMockCount = 0;

  public ScreenBuilder(EditorsScheme scheme) {
    myScheme = scheme;
  }

  public void addField(ServerFields.Field field) {
    FieldEditor editor = myScheme.getEditor(field);
    if (editor == null) {
      LogHelper.error("Missing editor", field);
      return;
    }
    addEditor(editor, field.getJiraId());
  }

  public void addMockEditor(FieldEditor editor) {
    String mockId = "mock." + myMockCount;
    myMockCount++;
    addEditor(editor, mockId);
  }

  private void addEditor(FieldEditor editor, String fieldId) {
    myFields.add(fieldId);
    myEditors.add(editor);
  }
  
  public MyScreen buildScreen(EditItemModel model) {
    ArrayList<String> fields = Collections15.arrayList(myFields);
    ArrayList<FieldEditor> editors = Collections15.arrayList(myEditors);
    for (int i = 0; i < fields.size(); i++) ResolvedField.addEditor(model, fields.get(i), editors.get(i));
    return new MyScreen(fields, editors);
  }

  public ScreenSet buildScreenSet() {
    return new ScreenSet() {
      private final TypedKey<IssueScreen> SCREEN = TypedKey.create("moveScreen");

      @Nullable
      @Override
      public List<FieldEditor> install(VersionSource source, EditItemModel model) {
        MyScreen screen = buildScreen(model);
        model.putHint(SCREEN, screen);
        return screen.getEditors();
      }

      @Nullable
      @Override
      public JComponent getScreenSelector(EditItemModel model) {
        return null;
      }

      @Override
      public boolean attach(Lifespan life, ScreenController controller, Configuration config) {
        IssueScreen screen = controller.getModel().getValue(SCREEN);
        if (screen == null) return false;
        controller.setScreen(screen);
        return true;
      }
    };
  }

  private static class MyScreen extends IssueScreen {
    private final ArrayList<String> myFields;
    private final ArrayList<FieldEditor> myEditors;

    public MyScreen(ArrayList<String> fields, ArrayList<FieldEditor> editors) {
      myFields = fields;
      myEditors = editors;
    }

    public ArrayList<FieldEditor> getEditors() {
      return myEditors;
    }

    @Override
    public List<Tab> getTabs(EditModelState model) {
      return Collections.singletonList(new Tab("", myFields));
    }

    @Nullable
    @Override
    public Object checkModelState(EditModelState model, @Nullable Object prevState) {
      return null;
    }
  }
}
