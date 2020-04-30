package com.almworks.jira.provider3.custom.customize;

import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.LoadAllFields;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.jira.provider3.custom.loadxml.FieldKeysLoader;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.DialogEditor;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.*;

class CustomFieldTypesEditor implements DialogEditor {
  static final String SAMPLE_KEY = "com.atlassian.jira.plugin.system.customfieldtypes:readonlyfield";
  static final CanvasRenderer<String> KEY_RENDERER = new CanvasRenderer<String>() {
    @Override
    public void renderStateOn(CellState state, Canvas canvas, String item) {
      Pair<String, String> keyPrefix = splitKey(item);
      canvas.appendText(keyPrefix.getFirst());
      String prefix = keyPrefix.getSecond();
      if (prefix.isEmpty()) return;
      CanvasSection section = canvas.emptySection();
      if (!state.isSelected()) section.setForeground(ColorUtil.between(state.getBackground(), state.getForeground(), 0.5f));
      section.appendText(" (").appendText(prefix).appendText(")");
    }
  };
  static final Comparator<String> KEY_COMPARATOR = new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
      Pair<String, String> kp1 = splitKey(o1);
      Pair<String, String> kp2 = splitKey(o2);
      int cmp = kp1.getFirst().compareTo(kp2.getFirst());
      if (cmp != 0) return cmp;
      return kp1.getSecond().compareTo(kp2.getSecond());
    }
  };

  private static final TableColumnAccessor<Map<TypedKey<?>,?>,String> COLUMN_TYPE_KEY =
    createStringColumn(ConfigKeys.KEY, "Key", SAMPLE_KEY)
      .setValueComparator(KEY_COMPARATOR)
      .setValueCanvasRenderer(KEY_RENDERER)
      .createColumn();

  private static final TableColumnAccessor<Map<TypedKey<?>,?>,String> COLUMN_TYPE_TYPE = new TableColumnBuilder<Map<TypedKey<?>, ?>, String>()
    .setConvertor(new Convertor<Map<TypedKey<?>, ?>, String>() {
      @Override
      public String convert(Map<TypedKey<?>, ?> value) {
        String type = ConfigKeys.TYPE.getFrom(value);
        boolean editable = ConfigKeys.EDITABLE.getFrom(value) != null;
        return type + (editable ? ", editable" : ", read-only");
      }
    })
    .setSizePolicy(new ColumnSizePolicy.Calculated(SizeCalculator1D.text("cascade editable WWW"), ColumnSizePolicy.FREE))
    .setValueComparator(String.CASE_INSENSITIVE_ORDER)
    .setValueCanvasRenderer(Renderers.defaultCanvasRenderer())
    .setId("Type")
    .createColumn();

  private static final TableColumnAccessor<Map<TypedKey<?>,?>,String> COLUMN_FIELD_NAME = createStringColumn(LoadAllFields.NAME, "Name", "Custom Field Name").createColumn();
  private static final TableColumnAccessor<Map<TypedKey<?>,?>,String> COLUMN_FIELD_CONNECTIN = createStringColumn(LoadAllFields.ConnectionName.CONNECTION_NAME, "Connection", "ConnectionConnection - ProjectProject").createColumn();
  public static final Convertor<TypedKey<?>,String> GET_KEY_NAME = new Convertor<TypedKey<?>, String>() {
    @Override
    public String convert(TypedKey<?> value) {
      return value != null ? value.getName() : null;
    }
  };

  private final SyncManager mySyncManager;
  private final CustomFieldsComponent myCustomFields;
  private final FieldTypesState myTypes;
  private JPanel myWholePanel;
  private JTextArea myXml;
  private ASortedTable<Map<TypedKey<?>,?>> myFieldTypes;
  private ASortedTable<Map<TypedKey<?>,?>> myFields;
  private JSplitPane myOuterSplit;
  private JSplitPane myInnerSplit;
  private JButton myUpdateButton;

  private CustomFieldTypesEditor(SyncManager syncManager, CustomFieldsComponent customFields, FieldTypesState typesState) {
    myOuterSplit.setBorder(null);
    myInnerSplit.setBorder(null);
    mySyncManager = syncManager;
    myCustomFields = customFields;
    myTypes = typesState;
  }

  public static void showEditor(DialogEditorBuilder builder, List<Map<TypedKey<?>, ?>> initial,
    LoadAllFields allFields) {
    FieldTypesState typesState = new FieldTypesState(initial);
    MutableComponentContainer container = builder.getWindowContainer();
    SyncManager syncManager = container.getActor(SyncManager.ROLE);
    CustomFieldsComponent customFields = container.getActor(CustomFieldsComponent.ROLE);
    final CustomFieldTypesEditor editor = new CustomFieldTypesEditor(syncManager, customFields, typesState);
    editor.myFieldTypes.setColumnModel(FixedListModel.<TableColumnAccessor<Map<TypedKey<?>,?>,?>>create(COLUMN_TYPE_TYPE, COLUMN_TYPE_KEY, typesState.getFieldCountColumn(allFields)));
    editor.myFields.setColumnModel(FixedListModel.<TableColumnAccessor<Map<TypedKey<?>,?>,?>>create(COLUMN_FIELD_NAME, COLUMN_FIELD_CONNECTIN, typesState.getFieldKeyColumn()));
    editor.myFieldTypes.getSelectionAccessor().addAWTChangeListener(new ChangeListener() {
      @Override
      public void onChange() {
        editor.updateXml();
      }
    });
    FieldTypeController.install(editor.myFieldTypes, editor.myFields, typesState, allFields);
    editor.myUpdateButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editor.doUpdateFields();
      }
    });
    builder.setContent(editor);
    builder.hideApplyButton();
    builder.showWindow();
  }

  private void doUpdateFields() {
    DefaultActionContext context = new DefaultActionContext(myUpdateButton);
    try {
      updateFields(context);
    } catch (CantPerformException e1) {
      // ignore
    }
  }

  private void updateFields(DefaultActionContext context) throws CantPerformException {
    final JTextArea text = new JTextArea();
    text.setColumns(80);
    text.setRows(5);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    JPanel panel = new JPanel(UIUtil.createBorderLayout());
    panel.add(new JLabel("Enter definition:"), BorderLayout.NORTH);
    panel.add(new JScrollPane(text), BorderLayout.CENTER);
    DialogManager manager = context.getSourceObject(DialogManager.ROLE);
    DialogBuilder builder = manager.createBuilder("editCustomFieldTypes.updateDialog");
    builder.setContent(panel);
    builder.setEmptyOkAction();
    builder.setEmptyCancelAction();
    class OkPressed implements AnActionListener {
      private boolean myPressed = false;

      @Override
      public void perform(ActionContext context) throws CantPerformException {
        myPressed = true;
      }
    }
    OkPressed pressed = new OkPressed();
    builder.addOkListener(pressed);
    builder.setTitle("Update Field Types");
    builder.setInitialFocusOwner(text);
    builder.setModal(true);
    builder.showWindow();
    if (!pressed.myPressed) return;
    updateTypes(text.getText());
  }

  private void updateTypes(String rawInput) {
    FixXml fix = FixXml.fixXml(rawInput);
    if (fix == null) {
      DialogsUtil.showErrorMessage(myUpdateButton, "Unexpected error occurred", "Update Field Types");
      return;
    }
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    SAXException problem = fix.getProblem();
    if (problem != null) {
      DialogsUtil.showErrorMessage(myUpdateButton, problem.getMessage(), "Update Field Types");
      return;
    }
    String xml = fix.getFixed();
    if (xml == null) {
      DialogsUtil.showErrorMessage(myUpdateButton, "Unexpected error occurred", "Update Field Types");
      return;
    }
    FieldKeysLoader loader = null;
    try {
      loader = FieldKeysLoader.load(new InputSource(new StringReader(xml)), CustomFieldsComponent.SCHEMA);
    } catch (SAXException e) {
      DialogsUtil.showErrorMessage(myUpdateButton, e.getMessage(), "Update Field Types");
    } catch (ParserConfigurationException e) {
      LogHelper.error(e);
      DialogsUtil.showErrorMessage(myUpdateButton, "Unexpected error occurred", "Update Field Types");
    } catch (IOException e) {
      LogHelper.error(e);
      DialogsUtil.showErrorMessage(myUpdateButton, "Unexpected error occurred", "Update Field Types");
    }
    if (loader != null) myTypes.update(loader.getLoadedKinds());
  }

  private void updateXml() {
    Map<TypedKey<?>, ?> type = myFieldTypes.getSelectionAccessor().getSelection();
    if (type == null) {
      myXml.setText("");
      return;
    }
    StringBuilder xml = new StringBuilder("<field");
    appendAttributes(xml, type, ConfigKeys.EDITABLE);
    Map<TypedKey<?>, ?> editable = ConfigKeys.EDITABLE.getFrom(type);
    if (editable == null) xml.append("/>");
    else {
      xml.append(">\n  <editable");
      appendAttributes(xml, editable);
      xml.append("/>\n</field>");
    }
    myXml.setText(xml.toString());
  }

  private static void appendAttributes(StringBuilder target, Map<TypedKey<?>, ?> map, TypedKey<?> ... exclude) {
    ArrayList<TypedKey<?>> keys = Collections15.arrayList(map.keySet());
    Collections.sort(keys, Containers.convertingComparator(GET_KEY_NAME, Containers.comparablesComparator()));
    for (TypedKey<?> key : keys) {
      if (ArrayUtil.indexOf(exclude, key) >= 0) continue;
      target.append(" ").append(key.getName()).append("=\"").append(key.getFrom(map)).append("\"");
    }
  }

  @Override
  public Modifiable getModifiable() {
    return myTypes.getModifiable();
  }

  @Override
  public boolean isModified() {
    return myTypes.isModified();
  }

  @Override
  public void apply() throws CantPerformExceptionExplained {
    try {
      myCustomFields.updateFields(mySyncManager, myTypes.getCurrent(), null); // todo accept notification
    } catch (FieldType.CreateProblem createProblem) {
      throw new CantPerformExceptionExplained(createProblem.getMessage(), "Update Field Types");
    }
  }

  @Override
  public void reset() {
    myTypes.reset();
  }

  @Override
  public JComponent getComponent() {
    return myWholePanel;
  }

  @Override
  public void dispose() {
  }

  static class GetFromMap extends Convertor<Map<TypedKey<?>, ?>, String> {
    private final TypedKey<String> myKey;

    public GetFromMap(TypedKey<String> key) {
      myKey = key;
    }

    @Override
    public String convert(Map<TypedKey<?>, ?> value) {
      return Util.NN(myKey.getFrom(value));
    }
  }

  static TableColumnBuilder<Map<TypedKey<?>, ?>, String> createStringColumn(TypedKey<String> mapKey, String name, String sampleValue) {
    return new TableColumnBuilder<Map<TypedKey<?>, ?>, String>().setConvertor(new GetFromMap(mapKey)).setValueComparator(String.CASE_INSENSITIVE_ORDER)
      .setSizePolicy(new ColumnSizePolicy.Calculated(SizeCalculator1D.text(sampleValue), ColumnSizePolicy.FREE))
      .setValueCanvasRenderer(Renderers.defaultCanvasRenderer())
      .setId(name);
  }

  static Pair<String, String> splitKey(String key) {
    if (key == null) return Pair.create("", "");
    int colonIndex = key.lastIndexOf(':');
    if (colonIndex < 0) colonIndex = key.lastIndexOf('.');
    if (colonIndex < 0 || colonIndex == key.length() - 1) return Pair.create(key, "");
    else return Pair.create(key.substring(colonIndex + 1), key.substring(0, colonIndex));
  }
}
