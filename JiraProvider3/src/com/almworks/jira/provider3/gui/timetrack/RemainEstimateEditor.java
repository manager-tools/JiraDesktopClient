package com.almworks.jira.provider3.gui.timetrack;

import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.composition.DelegatingFieldEditor;
import com.almworks.items.gui.edit.editors.text.ScalarFieldEditor;
import com.almworks.items.gui.edit.merge.BaseScalarMergeValue;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.gui.timetrack.edit.EditWorklogsFeature;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;

public class RemainEstimateEditor extends DelegatingFieldEditor<ScalarFieldEditor<Integer>> {
  private static final ScalarFieldEditor<Integer> DELEGATE = ScalarFieldEditor.timeInterval(NameMnemonic.rawText("Estimate"), Issue.REMAIN_ESTIMATE, EditWorklogsFeature.DEFAULT_UNIT, false);
  public static final RemainEstimateEditor INSTANCE = new RemainEstimateEditor();

  private RemainEstimateEditor() {
  }

  @Override
  protected ScalarFieldEditor<Integer> getDelegate(VersionSource source, EditModelState model) {
    return DELEGATE;
  }

  @Override
  public void commit(CommitContext context) {
    ModelWrapper<ScalarFieldEditor<Integer>> wrapper = getWrapperModel(context.getModel());
    if (wrapper == null) return;
    Integer newValue = wrapper.getEditor().getCurrentValue(wrapper);
    ItemVersionCreator issue = context.getDrain().unsafeChange(context.getItem());
    TimeUtils.commitExplicitRemain(issue, newValue, false);
  }

  public void attachComponent(Lifespan life, EditItemModel model, JTextField field) {
    ModelWrapper<ScalarFieldEditor<Integer>> wrapper = getWrapperModel(model);
    if (wrapper == null) return;
    wrapper.getEditor().attachComponent(life, wrapper, field);
  }

  private static class MyMergeValue extends BaseScalarMergeValue<Integer> {
    public MyMergeValue(String displayName, long item, Integer[] values, EditItemModel model) {
      super(displayName, item, values, model);
    }

    @Override
    protected void doSetResolution(int version) {
      DELEGATE.setValue(getModel(), getValue(version));
    }

    @Override
    protected FieldEditor getEditor() {
      return INSTANCE;
    }

    @Override
    public void render(CellState state, Canvas canvas, int version) {
      Integer value = getValue(version);
      String text = DELEGATE.convertToText(value);
      if (text != null) canvas.appendText(text);
    }
  }
}
