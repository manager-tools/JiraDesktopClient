package com.almworks.export;

import com.almworks.api.application.util.ItemExport;
import com.almworks.api.engine.Connection;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.AppBook;
import com.almworks.util.advmodel.*;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.config.Configuration;
import com.almworks.util.i18n.LText;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

class ExportDialog {
  private static final String X = "exportDialog";
  private static final LText DIALOG_TITLE = AppBook.text(X + "DIALOG_TITLE", "Export");
  private static final LText PERFORM_ACTION_NAME = AppBook.text(X + "PERFORM_ACTION_NAME", "&Export");
  private static final LText ATTRIBUTES_ACTION_NAME =
    AppBook.text("ExportForm.exportedAttributes", "Exported &Attributes\u2026");
  private static final LText FORMATS_ACTION_NAME =
    AppBook.text("ExportForm.formats", "&Formats\u2026");

  private final Configuration myConfiguration;
  private final ExportedData myData;
  private final List<ExporterDescriptor> myExporters;
  private final DialogManager myDialogManager;
  private final ExportForm myForm = new ExportForm();
  private final DetachComposite myLife = new DetachComposite();
  private final ExportParameters myTarget = new ExportParameters();
  private final SubsetModel<ItemExport> myAttributesModel;
  private final ExportAttributesForm myAttributesForm;
  private final ExportFormatsDialog myFormatsDialog;

  private boolean myConfirmed = false;
  private static final String LAST_EXPORTER_SETTING = "lastExporter";
  private static final String ATTRIBUTES_CONFIG = "attributes";
  private static final String ATTRIBUTES_CONFIG_INCLUDE = "include";

  public ExportDialog(Configuration configuration, ExportedData data, List<ExporterDescriptor> exporters,
    DialogManager dialogManager)
  {
    myConfiguration = configuration;
    myData = data;
    myExporters = exporters;
    myDialogManager = dialogManager;
    myAttributesModel = createAttributesModel(configuration, data);
    myAttributesForm = new ExportAttributesForm(myDialogManager, myAttributesModel, data.getSelectedColumnsNames());
    myFormatsDialog = new ExportFormatsDialog(myDialogManager, configuration);
    listenAttributesModel();
    listenFormats();
    listenForExportDetails();
    myForm.setAttributesAction(new EnabledAction(ATTRIBUTES_ACTION_NAME.format()) {
      protected void doPerform(ActionContext context) throws CantPerformException {
        editAttributes();
      }
    });
    myForm.setFormatsAction(new EnabledAction(FORMATS_ACTION_NAME.format()) {
      protected void doPerform(ActionContext context) throws CantPerformException {
        editFormats();
      }
    });
  }

  private void listenForExportDetails() {
    myTarget.addAWTChangeListener(myLife, new ChangeListener() {
      public void onChange() {
        updateExportDetails();
      }
    });
    updateExportDetails();
  }

  private void updateExportDetails() {
    StringBuffer buf = new StringBuffer("<html><body>");
    buf.append("<b>Exporting:</b> ").append(ExportUtils.getTotals(myData.getRecords())).append(".<br>");
    buf.append(getAttributesString()).append("<br>");
    appendFormatDetails(buf);
    myForm.setExportDetails(buf.toString());
  }

  private void appendFormatDetails(StringBuffer buf) {
    Locale locale = myTarget.getLocale();
    buf.append("<b>Number format:</b> ");
    NumberFormat number = myTarget.getNumberFormat();
    if (number == null) {
      buf.append("N/A");
    } else {
      if (number instanceof DecimalFormat) {
        buf.append(((DecimalFormat) number).toPattern());
        if (locale != null) {
          buf.append(" (").append(locale.getDisplayName(Locale.US)).append(")");
        }
      } else {
        buf.append(number.toString());
      }
    }
    buf.append("<br>");
    buf.append("<b>Date format:</b> ");
    DateFormat date = myTarget.getDateFormat();
    if (date == null) {
      buf.append("N/A");
    } else {
      if (date instanceof SimpleDateFormat) {
        buf.append(((SimpleDateFormat) date).toPattern());
        if (locale != null) {
          buf.append(" (" + locale.getDisplayName(Locale.US) + ")");
        }
      } else {
        buf.append(date.toString());
      }
    }
  }

  private String getAttributesString() {
    Set<ItemExport> keys = myTarget.getKeys();
    StringBuffer b = new StringBuffer();
    for (ItemExport key : keys) {
      if (b.length() > 0)
        b.append(", ");
      b.append(key.getDisplayName());
    }
    if (b.length() == 0)
      b.append("none selected");
    b.insert(0, "<b>Attributes: </b>");
    b.append('.');
    return b.toString();
  }

  private void listenFormats() {
    ScalarModel.Adapter listener = new ScalarModel.Adapter<Object>() {
      public void onScalarChanged(ScalarModelEvent<Object> event) {
        updateFormats();
      }
    };
    myFormatsDialog.getDateFormatModel().getEventSource().addStraightListener(myLife, listener);
    myFormatsDialog.getNumberFormatModel().getEventSource().addStraightListener(myLife, listener);
    myFormatsDialog.getLocaleModel().getEventSource().addStraightListener(myLife, listener);
    updateFormats(); // not required, but...
  }

  private void updateFormats() {
    myTarget.setDateFormat(myFormatsDialog.getDateFormatModel().getValue());
    myTarget.setNumberFormat(myFormatsDialog.getNumberFormatModel().getValue());
    myTarget.setLocale(myFormatsDialog.getLocaleModel().getValue());
  }

  private void listenAttributesModel() {
    ChangeListener listener = new ChangeListener() {
      public void onChange() {
        List<ItemExport> keys = myAttributesModel.toList();
        List<String> names = ItemExport.GET_ID.collectList(keys);
        myConfiguration.getOrCreateSubset(ATTRIBUTES_CONFIG).setSettings(ATTRIBUTES_CONFIG_INCLUDE, names);
        myTarget.setKeys(keys);
      }
    };
    myAttributesModel.addChangeListener(myLife, listener);
    listener.onChange();
  }

  private SubsetModel<ItemExport> createAttributesModel(
    Configuration config, ExportedData data)
  {
    final LinkedHashSet<ItemExport> keys = data.getKeys();
    final Collection<Connection> conns = data.getConnections();

    final OrderListModel<ItemExport> exportableKeys = OrderListModel.create();
    for(final ItemExport key : keys) {
      if(key.isExportable(conns)) {
        exportableKeys.addElement(key);
      }
    }

    final SubsetModel<ItemExport> subsetModel = SubsetModel.create(myLife, exportableKeys, false);

    final Configuration c = config.getOrCreateSubset(ATTRIBUTES_CONFIG);
    final List<String> names = c.getAllSettings(ATTRIBUTES_CONFIG_INCLUDE);
    if(names.size() > 0) {
      addKeys(subsetModel, keys, ItemExport.GET_ID, names);
    } else {
      addKeys(subsetModel, keys, ItemExport.GET_DISPLAY_NAME, data.getSelectedColumnsNames());
    }

    return subsetModel;
  }

  private static void addKeys(SubsetModel<ItemExport> subsetModel, LinkedHashSet<ItemExport> allKeys,
    Convertor<ItemExport, String> convertor, List<String> names)
  {
    Map<String, ItemExport> map = convertor.assignKeys(allKeys);
    for (String name : names) {
      ItemExport key = map.get(name);
      if (key != null) {
        if (subsetModel.getFullSet().indexOf(key) >= 0) {
          subsetModel.add(key);
        }
      }
    }
  }


  private void editAttributes() {
    myAttributesForm.show();
  }

  private void editFormats() {
    myFormatsDialog.show();
  }

  public ExportParameters collectParameters(boolean silent) {
    try {
      setupExporters();
      if (!silent) {
        DialogBuilder builder = createDialog();
        if (myAttributesModel.getSize() == 0) ExportAttributesForm.setupFieldsFromTable(myAttributesModel, myData.getSelectedColumnsNames());
        builder.showWindow();
        if (!myConfirmed)
          return null;
      }
      Exporter exporter = myForm.getSelectedExporter();
      if (exporter == null) {
        assert false;
        return null;
      }
      myTarget.setExporter(exporter);
      exporter.getUI().addParametersTo(myTarget);
      return myTarget;
    } finally {
      myLife.detach();
    }
  }

  private void setupExporters() {
    List<Exporter> exporters = Collections15.arrayList(myExporters.size());
    for (ExporterDescriptor descriptor : myExporters) {
      try {
        Configuration exporterConfig = myConfiguration.getOrCreateSubset(descriptor.getKey());
        exporters.add(descriptor.createExporter(exporterConfig, myTarget, myData));
      } catch (ExporterNotApplicableException e) {
        // exporter is not applicable to data
        Log.debug(e);
      }
    }
    SelectionInListModel<Exporter> comboModel =
      SelectionInListModel.create(myLife, FixedListModel.create(exporters), getLastExporter(exporters));
    watchLastExplorer(comboModel);
    myForm.setExporters(comboModel);
  }

  private void watchLastExplorer(final SelectionInListModel<Exporter> comboModel) {
    comboModel.addSelectionListener(myLife, new SelectionListener.SelectionOnlyAdapter() {
      public void onSelectionChanged() {
        Exporter exporter = comboModel.getSelectedItem();
        if (exporter != null) {
          myConfiguration.setSetting(LAST_EXPORTER_SETTING, exporter.getDescriptor().getKey());
        }
      }
    });
  }

  private Exporter getLastExporter(List<Exporter> exportersModel) {
    String lastExporter = myConfiguration.getSetting(LAST_EXPORTER_SETTING, null);
    Exporter selectedExporter = null;
    if (lastExporter != null) {
      for (Exporter exporter : exportersModel) {
        if (exporter.getDescriptor().getKey().equals(lastExporter)) {
          selectedExporter = exporter;
          break;
        }
      }
    }
    if (selectedExporter == null && exportersModel.size() > 0) {
      selectedExporter = exportersModel.get(0);
    }
    return selectedExporter;
  }

  private DialogBuilder createDialog() {
    DialogBuilder builder = myDialogManager.createBuilder("export");
    builder.setContent(myForm);
    builder.setTitle(DIALOG_TITLE.format());
    builder.setModal(true);
    builder.setEmptyCancelAction();
    final JLabel errorLabel = new JLabel();
    errorLabel.setPreferredSize(UIUtil.getRelativeDimension(errorLabel, 40, 1));
    errorLabel.setForeground(GlobalColors.ERROR_COLOR);
    UIUtil.adjustFont(errorLabel, -1, Font.BOLD, false);
    builder.setBottomLineComponent(errorLabel);
    builder.setOkAction(new SimpleAction(PERFORM_ACTION_NAME.format()) {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        // todo remove updateExportAction from myForm and watch only myTarget
        StringBuffer error = new StringBuffer();
        try {
          myForm.updateExportAction(context, error);
          context.updateOnChange(myTarget);
          if (context.getEnabled() == EnableState.ENABLED) {
            if (myTarget.getKeysCount() == 0) {
              error.append("No attributes are selected");
              context.setEnabled(EnableState.DISABLED);
            } else if (myTarget.getDateFormat() == null) {
              error.append("Date format is not available");
              context.setEnabled(EnableState.DISABLED);
            } else if (myTarget.getNumberFormat() == null) {
              error.append("Number format is not available");
              context.setEnabled(EnableState.DISABLED);
            }
            context.updateOnChange(myTarget);
          }
        } finally {
          String text = error.toString();
          if (text.length() > 0)
            text = "Problem: " + text;
          errorLabel.setText(text);
        }
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        Exporter exporter = myForm.getSelectedExporter();
        assert exporter != null;
        if (exporter == null)
          throw new CantPerformException();
        exporter.confirmExport();
        myConfirmed = true;
      }
    });
    return builder;
  }
}
