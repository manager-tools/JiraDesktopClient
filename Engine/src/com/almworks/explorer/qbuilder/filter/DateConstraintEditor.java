package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintEditorNodeImpl;
import com.almworks.api.application.qb.ConstraintFilterNode;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.explorer.qbuilder.constraints.AbstractConstraintEditor;
import com.almworks.util.AppBook;
import com.almworks.util.L;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ADateField;
import com.almworks.util.components.ButtonSelectedListener;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.BooleanPropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.ComponentEnabler;
import com.almworks.util.ui.ComponentKeyBinder;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DefaultActionContext;
import org.almworks.util.Failure;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import static com.almworks.explorer.qbuilder.filter.EmptyQueryHelper.PK_EMPTY;

/**
 * @author dyoma
 */
class DateConstraintEditor extends AbstractConstraintEditor {
  static final String BOOK_PREFIX = "QueryEditor.DateNode";
  static final BooleanPropertyKey PRESET_MODE = BooleanPropertyKey.createKey("preset", true);
  static final BooleanPropertyKey CUSTOM_MODE = BooleanPropertyKey.createKey("custom", false);

  private final Form myForm = new Form();
  private static final String INVALID_CONSTRAINT_MESSAGE = "<Invalid>";

  public DateConstraintEditor(ConstraintEditorNodeImpl node) {
    super(node);

    final ComponentKeyBinder binder = getBinder();
    DateBoundParams.BEFORE.install(binder, myForm.myBefore);
    DateBoundParams.AFTER.install(binder, myForm.myAfter);
    binder.setBoolean(CUSTOM_MODE, myForm.myCustomInterval);
    binder.setBoolean(PRESET_MODE, myForm.myPresetInterval);
    binder.setBoolean(PK_EMPTY, myForm.myIsEmpty);
    binder.setCombobox(DatePreset.KEY, myForm.myPresets);

    myForm.myPresets.getModel().addSelectionListener(Lifespan.FOREVER,
      new SelectionListener.Adapter() {
        @Override
        public void onSelectionChanged() {
          updatePresetValues();
        }
      });

    new ButtonSelectedListener(myForm.myPresetInterval) {
      @Override
      protected void selectionChanged() {
        updatePresetValues();
      }
    }.attach();

    myForm.mySetDate.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          askDate(new DefaultActionContext(e));
        } catch (CantPerformException e1) {
          throw new Failure(e1);
        }
      }
    });

    if(!getBooleanValue(PRESET_MODE) && !getBooleanValue(CUSTOM_MODE) && !getBooleanValue(PK_EMPTY)) {
      myForm.myPresetInterval.setSelected(true);
    }
  }

  @Override
  public boolean isModified() {
    if(wasChanged(CUSTOM_MODE, PRESET_MODE, PK_EMPTY)) {
      return true;
    }
    if(getBooleanValue(PK_EMPTY)) {
      return false;
    }
    if(getBooleanValue(PRESET_MODE)) {
      return wasChanged(DatePreset.KEY);
    }
    return DateBoundParams.BEFORE.isModified(this) || DateBoundParams.AFTER.isModified(this);
  }

  @Override
  @NotNull
  public FilterNode createFilterNode(ConstraintDescriptor descriptor) {
    final PropertyMap values;
    if(getBooleanValue(PK_EMPTY)) {
      values = EmptyQueryHelper.createEmptyValues();
    } else {
      final boolean isPreset = getBooleanValue(PRESET_MODE);
      values = DateAttribute.createValues(
        DateBoundParams.BEFORE.getValue(getBinding()),
        DateBoundParams.AFTER.getValue(getBinding()),
        isPreset ? getValue(DatePreset.KEY) : null);
    }
    return new ConstraintFilterNode(descriptor, values);
  }

  @Override
  public void renderOn(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
    if(getBooleanValue(PK_EMPTY)) {
      renderEmpty(canvas, descriptor);
    } else if(getBooleanValue(PRESET_MODE)) {
      renderPreset(canvas, descriptor);
    } else if(getBooleanValue(CUSTOM_MODE)) {
      renderCustom(canvas, descriptor);
    }
  }

  private void renderEmpty(Canvas canvas, ConstraintDescriptor descriptor) {
    canvas.setIcon(Icons.QUERY_CONDITION_DATE_ATTR);
    canvas.appendText(descriptor.getDisplayName());
    canvas.appendText(" is empty");
  }

  private void renderPreset(Canvas canvas, ConstraintDescriptor descriptor) {
    canvas.setIcon(Icons.QUERY_CONDITION_DATE_PRESET);
    canvas.appendText(descriptor.getDisplayName());
    canvas.appendText(" ");
    DatePreset preset = getValue(DatePreset.KEY);
    if (preset != null) {
      canvas.appendText(L.special("during "));
      canvas.appendText(preset.getDisplayString());
    } else {
      canvas.appendText(L.special(INVALID_CONSTRAINT_MESSAGE));
    }
  }

  private void renderCustom(Canvas canvas, ConstraintDescriptor descriptor) {
    String before = DateBoundParams.BEFORE.getDisplayString(getBinding());
    String after = DateBoundParams.AFTER.getDisplayString(getBinding());
    canvas.setIcon(Icons.QUERY_CONDITION_DATE_ATTR);
    canvas.appendText(descriptor.getDisplayName());
    canvas.appendText(" in (");
    if (after != null || before != null) {
      appendDate(canvas, after, DateBoundParams.AFTER);
      canvas.appendText(", ");
      appendDate(canvas, before, DateBoundParams.BEFORE);
    } else {
      canvas.appendText(L.special(INVALID_CONSTRAINT_MESSAGE));
    }
    canvas.appendText(")");
  }

  private void appendDate(Canvas canvas, String date, DateBoundParams param) {
    boolean absolute = param.isAbsolute(getBinding());
    canvas.appendText(date != null ? date + (absolute ? "" : " ago") : "-");
  }

  @Override
  public JComponent getComponent() {
    return myForm.myWholePanel;
  }

  private void updatePresetValues() {
    if(getBooleanValue(PRESET_MODE)) {
      final DatePreset preset = getValue(DatePreset.KEY);
      if(preset != null) {
        setValues(preset.getValues());
      }
    }
  }

  private void askDate(ActionContext context) throws CantPerformException {
    DialogEditorBuilder builder = context.getSourceObject(DialogManager.ROLE).createEditor("QueryEditor.Date.SetDate");
    builder.setTitle(L.dialog("Set Date"));
    PropertyMap values = new PropertyMap();
    if (DateBoundParams.AFTER.isEnabled(getBinding()))
      SpecificDate.PARAMS.getCurrentValuesFrom(DateBoundParams.AFTER, getBinding(), values);
    else if (DateBoundParams.BEFORE.isEnabled(getBinding()))
      SpecificDate.PARAMS.getCurrentValuesFrom(DateBoundParams.BEFORE, getBinding(), values);
    else
      SpecificDate.PARAMS.setInitialValue(new DateUnit.AbsoluteDate(new Date()), values);
    SpecificDate dateEditor = new SpecificDate(values);
    builder.setContent(dateEditor);
    builder.setModal(true);
    builder.showWindow();
    if (builder.getLastEvent() == DialogEditorBuilder.CANCEL_EVENT)
      return;
    DateBoundParams.AFTER.getValuesFrom(SpecificDate.PARAMS, values, getBinding());
    DateBoundParams.AFTER.setEnabled(getBinding());
    DateBoundParams.BEFORE.getValuesFrom(SpecificDate.PARAMS, values, getBinding());
    DateBoundParams.BEFORE.setEnabled(getBinding());
    myForm.myBefore.adjustDates();
    myForm.myAfter.adjustDates();
  }

  private static class Form {
    private ADateField myAfterDate;
    private JTextField myAfterRelative;
    private ADateField myBeforeDate;
    private JTextField myBeforeRelative;
    private JRadioButton myAfterRelativeRadio;
    private JRadioButton myAfterAbsoluteRadio;
    private JRadioButton myBeforeAbsoluteRadio;
    private JRadioButton myBeforeRelativeRadio;
    private AComboBox<DateUnit> myAfterUnits;
    private AComboBox<DateUnit> myBeforeUnits;
    private AComboBox<DatePreset> myPresets;
    private JCheckBox myBeforeUsed;
    private JCheckBox myAfterUsed;
    private JPanel myWholePanel;
    private JButton mySetDate;
    private JRadioButton myPresetInterval;
    private JRadioButton myCustomInterval;
    private JRadioButton myIsEmpty;

    private final DateBoundParams.Components myBefore;
    private final DateBoundParams.Components myAfter;

    public Form() {
      AppBook.replaceText(BOOK_PREFIX, myWholePanel);
      myBefore = new DateBoundParams.Components(myBeforeUsed, myBeforeRelativeRadio, myBeforeAbsoluteRadio,
        myBeforeRelative, myBeforeUnits, myBeforeDate);

      myAfter = new DateBoundParams.Components(myAfterUsed, myAfterRelativeRadio, myAfterAbsoluteRadio,
        myAfterRelative, myAfterUnits, myAfterDate);

      ComponentEnabler.create(myCustomInterval, myBeforeUsed, myAfterUsed, mySetDate);
      ComponentEnabler.create(myPresetInterval, myPresets);
      myPresets.setCanvasRenderer(DatePreset.RENDERER);

      final ButtonGroup group = new ButtonGroup();
      group.add(myPresetInterval);
      group.add(myCustomInterval);
      group.add(myIsEmpty);
    }

    private void createUIComponents() {
      myAfterDate = new ADateField(ADateField.Precision.DAY);
      myBeforeDate = new ADateField(ADateField.Precision.DAY);
    }
  }
}
