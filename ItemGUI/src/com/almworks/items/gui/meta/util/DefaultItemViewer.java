package com.almworks.items.gui.meta.util;

import com.almworks.engine.gui.CommonIssueViewer;
import com.almworks.engine.gui.ItemTableBuilder;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.ViewerFieldsCollector;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.config.Configuration;

import javax.swing.*;

public abstract class DefaultItemViewer extends CommonIssueViewer {
  private final GuiFeaturesManager myFeatures;
  protected final DBStaticObject myKeyKey;
  protected final DBStaticObject myKeySummary;

  protected DefaultItemViewer(Configuration config, GuiFeaturesManager features, DBStaticObject keyKey, DBStaticObject keySummary) {
    super(config);
    myFeatures = features;
    myKeyKey = keyKey;
    myKeySummary = keySummary;
  }

  @Override
  protected ViewerFieldsCollector getViewerFieldManager() {
    return myFeatures.getViewerFields();
  }

  protected JComponent createKeyPanel() {
    return createToStringKeyPanel("", myFeatures.findScalarKey(myKeyKey, Object.class));
  }

  protected JComponent createSummaryPanel() {
    return createSummaryPanel(myFeatures.findScalarKey(myKeySummary, String.class));
  }

  protected final GuiFeaturesManager getFeatures() {
    return myFeatures;
  }

  protected void addLeftSideFields(ItemTableBuilder fields) {
    getViewerFieldManager().addLeftFields(fields);
  }
}
