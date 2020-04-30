package com.almworks.explorer.qbuilder.editor;

import com.almworks.api.application.qb.FilterEditor;
import com.almworks.api.application.qb.QueryBuilderComponent;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.config.ConfigAccessors;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.DialogEditor;
import com.almworks.util.ui.TextEditor;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public class QueryBuilderComponentImpl implements QueryBuilderComponent {
  private static final String IMMEDIATELY_RUN_SETTING = "runQueryImmediately";
  private Configuration myConfig;

  public QueryBuilderComponentImpl(Configuration config) {
    myConfig = config;
  }

  public JCheckBox createRunImmediatlyCheckbox() {
    final JCheckBox runningImmediately = new JCheckBox(L.checkbox("Run " + Terms.query + " immediately"));
    UIUtil.bindCheckBox(runningImmediately, new ConfigAccessors.Bool(myConfig, IMMEDIATELY_RUN_SETTING, true));
    return runningImmediately;
  }

  public DialogEditor createQueryEditor(TextEditor nameEditor, FilterEditor filterEditor) {
    return new QueryEditor(nameEditor, filterEditor);
  }
}
