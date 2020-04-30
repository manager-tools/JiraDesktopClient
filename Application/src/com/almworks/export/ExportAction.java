package com.almworks.export;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.explorer.TableController;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.platform.ProductInformation;
import com.almworks.export.csv.CSVExporterDescriptor;
import com.almworks.export.html.HTMLExporterDescriptor;
import com.almworks.export.pdf.PDFExporterDescriptor;
import com.almworks.export.xml.XMLExporterDescriptor;
import com.almworks.util.AppBook;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.files.FileActions;
import com.almworks.util.i18n.LText;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.picocontainer.Startable;

import java.util.Collections;
import java.util.List;

public class ExportAction extends SimpleAction implements Startable {
  public static final Role<ExportAction> ROLE = Role.role(ExportAction.class);

  private static final String X = "ExportAction";
  private static final LText NAME = AppBook.text(X + ".actionName", "E&xport\u2026");
  private static final LText DESCRIPTION =
    AppBook.text(X + ".description", "Export currently displayed " + Terms.ref_artifacts + " table to a file");

  private final ActionRegistry myActionRegistry;
  private final ComponentContainer myContainer;
  private final List<ExporterDescriptor> myExporters;

  public ExportAction(ActionRegistry actionRegistry, ComponentContainer container, DialogManager dialogManager,
    WorkArea workArea, ProductInformation productInfo)
  {
    super(NAME.format(), Icons.ACTION_EXPORT);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, DESCRIPTION.format());
    myExporters = createExporterList(dialogManager, workArea, productInfo);
    myActionRegistry = actionRegistry;
    myContainer = container;
    watchRole(TableController.DATA_ROLE);
  }

  private static List<ExporterDescriptor> createExporterList(DialogManager dialogManager, WorkArea workArea, ProductInformation productInfo) {
    List<ExporterDescriptor> exporters = Collections15.arrayList();
    exporters.add(new CSVExporterDescriptor(dialogManager));
    exporters.add(new HTMLExporterDescriptor(dialogManager, workArea));
    exporters.add(new XMLExporterDescriptor(dialogManager, productInfo));
    exporters.add(new PDFExporterDescriptor(dialogManager, workArea));
    return Collections.unmodifiableList(exporters);
  }

  public void start() {
    myActionRegistry.registerAction(MainMenu.Tools.EXPORT, this);
    if (FileActions.isSupported(FileActions.Action.OPEN)) {
      myActionRegistry.registerAction(MainMenu.Tools.QUICK_EXPORT, new ExportToExcelAction());
    }
  }

  public void stop() {
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    doExport(context, false);
  }

  private void doExport(ActionContext context, boolean silent) throws CantPerformException {
    TableController controller = context.getSourceObject(TableController.DATA_ROLE);
    ExportedData data = ExportedData.create(controller);
    ExportParameters parameters = collectParameters(data, silent);
    if (parameters == null) {
      // cancelled
      return;
    }
    Exporter exporter = parameters.getExporter();
    if (exporter == null) {
      // ?
      return;
    }
    exporter.export(data, parameters, silent);
  }

  private ExportParameters collectParameters(ExportedData data, boolean silent) {
    MutableComponentContainer container = myContainer.createSubcontainer("export");
    container.registerActor(data);
    container.registerActor(myExporters);
    ExportDialog dialog = container.instantiate(ExportDialog.class);
    return dialog.collectParameters(silent);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    TableController controller = context.getSourceObject(TableController.DATA_ROLE);
    AListModel<? extends LoadedItem> model = controller.getCollectionModel();
    context.updateOnChange(model);
    int size = model.getSize();
    context.setEnabled(size > 0 ? EnableState.ENABLED : EnableState.DISABLED);
  }

  private class ExportToExcelAction extends SimpleAction {
    public ExportToExcelAction() {
      super("&Quick Export", Icons.ACTION_EXPORT_QUICK);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
        "Silently export with the last used parameters and open exported file");
      watchRole(TableController.DATA_ROLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      ExportAction.this.customUpdate(context);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      doExport(context, true);
    }
  }
}