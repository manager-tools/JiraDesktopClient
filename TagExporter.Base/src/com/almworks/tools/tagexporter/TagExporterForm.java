package com.almworks.tools.tagexporter;

import com.almworks.util.Env;
import com.almworks.util.L;
import com.almworks.util.collections.Functional;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.components.AActionButton;
import com.almworks.util.components.ALabel;
import com.almworks.util.components.FileSelectionField;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileActions;
import com.almworks.util.images.Icons;
import com.almworks.util.progress.Progress;
import com.almworks.util.tags.TagFileStorage;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.*;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static javax.swing.SwingUtilities.isDescendingFrom;

public class TagExporterForm {
  public static final String PRODUCT_ID_PROPERTY = "product.id";
  public static final String PRODUCT_ID_JIRACLIENT = "jiraclient";
  public static final String PRODUCT_ID_DESKZILLA = "deskzilla";

  private final TagExporterEnv myEnv;

  private final JPanel myMainPanel = new JPanel();
  private final FileSelectionField myWorkspaceChooser = new FileSelectionField();
  private final FileSelectionField myOutputFileChooser = new FileSelectionField();
  private final CardLayout myStatusLayout = new CardLayout();
  private static final String PROGRESS_CARD = "progress";
  private static final String ERROR_CARD = "error";
  private final JPanel myStatusPanel = new JPanel(myStatusLayout);

  private final AActionButton myExportButton = new AActionButton();

  private boolean myOutputFileSetManually = false;
  private boolean myAutosettingOutputFile = false;

  private final LinkedHashMap<String, String> myErrors = Collections15.linkedHashMap();
  private final JProgressBar myProgressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
  private final ALabel myErrorLabel = new ALabel();

  private final SimpleModifiable myModifiable = new SimpleModifiable();

  public static final TypedKey<TagExporter> TAG_EXPORTER_ROLE = TypedKey.create("TagExporter");
  public static final TypedKey<Modifiable> PROGRESS_MODIFIABLE_ROLE = TypedKey.create("ProgressModifiable");
  public static final TypedKey<Boolean> SUCCESS_ROLE = TypedKey.create("Success");
  private final SimpleProvider myDataProvider = new SimpleProvider(TAG_EXPORTER_ROLE, PROGRESS_MODIFIABLE_ROLE, SUCCESS_ROLE);
  private String myProductId;
  private String myAppLabelName;
  private String myAppTitleName;

  public TagExporterForm(TagExporterEnv env) {
    myEnv = env;
  }

  public static void showWindow(TagExporterEnv env) {
    JFrame frame = new JFrame();
    frame.setIconImage(Icons.APPLICATION_LOGO_ICON_BIG.getImage());
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null);
    frame.setResizable(false);

    WindowMinSizeWatcher.install(frame);

    TagExporterForm form = new TagExporterForm(env);
    form.readProductId();
    frame.setTitle(L.frame(form.myAppTitleName + " " + env.getFullName().trim()));

    form.init();
    frame.setContentPane(form.myMainPanel);
    frame.getRootPane().setDefaultButton(form.myExportButton);

    frame.pack();
    frame.setVisible(true);
  }

  public void init() {
    setupComponents();
    setupActions();
    setupListeners(myMainPanel);
    setInitialValues();
    updateStatus();
    setupLayout();
  }

  private void setupComponents() {
    JFileChooser workspaceFc = myWorkspaceChooser.getFileChooser();
    workspaceFc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    workspaceFc.setApproveButtonText("Select");
    workspaceFc.setDialogTitle(L.dialog("Select Workspace Folder"));
    highlight(myWorkspaceChooser.getField(), true);
    myWorkspaceChooser.setDoubleClickEnabled(false);

    myOutputFileChooser.getFileChooser().setApproveButtonText("Select");
    myOutputFileChooser.setDoubleClickEnabled(false);
    
    myErrorLabel.setForeground(GlobalColors.ERROR_COLOR);
  }

  private void setupActions() {
    DataProvider.DATA_PROVIDER.putClientValue(myMainPanel, myDataProvider);
    myDataProvider.setSingleData(SUCCESS_ROLE, false);
    myExportButton.setAnAction(new ExportTagsAction());
  }

  private void setInitialValues() {
    setDefaultWorkspace();
    myErrors.clear();
  }

  private void setDefaultWorkspace() {
    File userHome = new File(System.getProperty("user.home"));
    // If unknown product or no product, default to Deskzilla
    if (!PRODUCT_ID_JIRACLIENT.equals(myProductId)) {
      myProductId = PRODUCT_ID_DESKZILLA;
    }
    String workspaceDirInHome = PRODUCT_ID_JIRACLIENT.equals(myProductId) ? ".JIRAClient" : ".Deskzilla";
    File defaultWorkspace;
    try {
      defaultWorkspace = new File(userHome, workspaceDirInHome).getCanonicalFile();
    } catch (Exception e) {
      myWorkspaceChooser.getField().setToolTipText(
        L.tooltip("If in doubt, check for the path specified in " + myAppLabelName + "\u2019s main window header"));
      return;
    }
    myWorkspaceChooser.setFile(defaultWorkspace);
  }

  private void readProductId() {
    myProductId = System.getProperty(PRODUCT_ID_PROPERTY);
    if(PRODUCT_ID_JIRACLIENT.equals(myProductId)) {
      myAppLabelName = myAppTitleName = "Client for Jira";
    } else if(PRODUCT_ID_DESKZILLA.equals(myProductId)) {
      myAppLabelName = myAppTitleName = "Deskzilla";
    } else {
      myAppTitleName = "Deskzilla and Client for Jira";
      myAppLabelName = "Deskzilla or Client for Jira";
    }
  }

  private void setupLayout() {
    String columnSpecs = "d, 4dlu, fill:d:g";
    DefaultFormBuilder mainBuilder = new DefaultFormBuilder(new FormLayout(columnSpecs), myMainPanel);

    mainBuilder.append("1.");
    JLabel workspaceChooserLabel = new JLabel("Select the folder where your " + myAppLabelName + " workspace resides:");
    mainBuilder.append(workspaceChooserLabel);
    mainBuilder.append("");
    mainBuilder.append(myWorkspaceChooser);
    workspaceChooserLabel.setDisplayedMnemonicIndex(workspaceChooserLabel.getText().indexOf("workspace"));
    workspaceChooserLabel.setLabelFor(myWorkspaceChooser);

    mainBuilder.append("2.");
    JLabel outFileLabel = new JLabel("Select a file to export your tags to:");
    outFileLabel.setLabelFor(myOutputFileChooser);
    mainBuilder.append(outFileLabel);
    mainBuilder.append("");
    mainBuilder.append(myOutputFileChooser);

    mainBuilder.append("3.");
    mainBuilder.append("<html>Click <b>Export</b> and wait a little.");
    mainBuilder.append("");
    JPanel exportPanel = new JPanel(UIUtil.createBorderLayout());
    exportPanel.add(myExportButton, BorderLayout.WEST);
    myStatusPanel.add(myProgressBar, PROGRESS_CARD);
    myStatusPanel.add(myErrorLabel, ERROR_CARD);
    myStatusLayout.show(myStatusPanel, PROGRESS_CARD);
    exportPanel.add(myStatusPanel, BorderLayout.CENTER);
    mainBuilder.append(exportPanel);

    mainBuilder.append("4.");
    mainBuilder.append("You can check the result when it's ready, back it up, or share it.");
    mainBuilder.append("");
    JPanel resultPanel = new JPanel(InlineLayout.horizontal(UIUtil.GAP));
    resultPanel.add(new AActionButton(new ShowInFolderAction()));
    resultPanel.add(new AActionButton(new OpenFileAction()));
    mainBuilder.append(resultPanel);

    mainBuilder.append("5.");
    mainBuilder.append("<html>Launch " + myAppLabelName + " 3.0 and use <b>Tools | Import Tags\u2026</b>");
    mainBuilder.append("");

    myMainPanel.setBorder(Borders.DLU4_BORDER);
    myMainPanel.setPreferredSize(new Dimension(450, myMainPanel.getPreferredSize().height));
  }

  private void setupListeners(final JComponent globalParent) {
    myOutputFileChooser.getField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void documentChanged(DocumentEvent e) {
        if (!myAutosettingOutputFile)
          myOutputFileSetManually = true;
        setError(TagExporterEnv.OUT_FILE_KEY, null);
      }
    });
    myOutputFileChooser.getFileChooser().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
          setError(TagExporterEnv.OUT_FILE_KEY, null);
        }
      }
    });
    listenFocus(myOutputFileChooser.getField(), globalParent, new Runnable() { public void run() {
      myAutosettingOutputFile = true;
      setCanonicalOutputFile(myOutputFileChooser.getField().getText());
      myAutosettingOutputFile = false;
    }});
    myWorkspaceChooser.getField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void documentChanged(DocumentEvent e) {
        updateOutputFileChooser();
        setError(TagExporterEnv.WORKSPACE_KEY, null);
      }
    });
    myWorkspaceChooser.getFileChooser().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
          verifyWorkspace(myWorkspaceChooser.getFileChooser().getSelectedFile());
        }
      }
    });
    listenFocus(myWorkspaceChooser.getField(), globalParent, new Runnable() { public void run() {
      verifyWorkspace(new File(myWorkspaceChooser.getField().getText()));
    }});
  }

  private static void listenFocus(final JTextComponent component, final Container globalParent, final Runnable onFocusLostInWindow) {
    component.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        Component newOwner = e.getOppositeComponent();
        if (newOwner != null && isDescendingFrom(newOwner, globalParent)) {
          // focus switched from the text field somewhere in the same window: to prevent verification when file chooser is opened
          onFocusLostInWindow.run();
        }
      }
    });
  }

  private boolean verifyWorkspace(File workspace) {
    String error = myEnv.verifyWorkspace(workspace);
    setError(TagExporterEnv.WORKSPACE_KEY, error);
    return error == null;
  }

  private void updateOutputFileChooser() {
    if (!myOutputFileSetManually) {
      File workspaceDir = myWorkspaceChooser.getFile();
      myAutosettingOutputFile = true;
      myOutputFileChooser.setFile(new File(workspaceDir, TagFileStorage.DEFAULT_FILE_NAME));
      myAutosettingOutputFile = false;
      setError(TagExporterEnv.OUT_FILE_KEY, null);
    }
  }

  private void setCanonicalOutputFile(String outFile) {
    try {
      myOutputFileChooser.setFile(new File(outFile).getCanonicalFile());
    } catch (IOException e) {
      setError(TagExporterEnv.OUT_FILE_KEY, e.getMessage());
    }
  }

  private void updateStatus() {
    String message = myErrors.get(TagExporterEnv.GLOBAL_KEY);
    Map.Entry<String, String> localError = Functional.first(myErrors.entrySet());
    if(localError != null && message == null) {
      message = localError.getValue();
    }

    if(message != null) {
      myErrorLabel.setText(capitalize(message));
      myProgressBar.setValue(0);
      myStatusLayout.show(myStatusPanel, ERROR_CARD);
    } else {
      myStatusLayout.show(myStatusPanel, PROGRESS_CARD);
    }
    
    myModifiable.fireChanged();
  }

  private static String capitalize(String s) {
    return s.length() < 2 ? s : Util.upper(s.substring(0, 1)) + s.substring(1);
  }

  private static void highlight(final JTextComponent c, boolean requestFocus) {
    UIUtil.selectAll(c);
    if (requestFocus) UIUtil.requestFocusInWindowLater(c);
  }

  private void setError(String key, @Nullable String msg) {
    if (msg != null) myErrors.put(key, msg);
    else myErrors.remove(key);
    // it's done always because each more recent action clears global error
    if (key != TagExporterEnv.GLOBAL_KEY) myErrors.remove(TagExporterEnv.GLOBAL_KEY);
    updateStatus();
  }

  private class ExportTagsAction extends SimpleAction {
    private ExportTagsAction() {
      super("&Export");
      watchRole(TAG_EXPORTER_ROLE);
      watchModifiableRole(PROGRESS_MODIFIABLE_ROLE);
      watchRole(SUCCESS_ROLE);
      updateOnChange(myModifiable);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      TagExporter tagExporter = ActionUtil.getNullable(context, TAG_EXPORTER_ROLE);
      if (tagExporter != null) {
        updateProgress(tagExporter);
      }

      boolean notInProgress = tagExporter == null || tagExporter.getProgress().isDone();
      myWorkspaceChooser.setEnabled(notInProgress);
      myOutputFileChooser.setEnabled(notInProgress);
      context.setEnabled(notInProgress);

      CantPerformException.ensureNotNull(myOutputFileChooser.getFile());
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      File workArea = getWorkArea();
      File outFile = getOutputFile();
      Progress progress = new Progress("Exporting\u2026");

      TagExporter tagExporter = TagExporter.exportTags(myEnv, workArea, outFile, progress, new Procedure2<String, String>() {
        @Override
        public void invoke(String key, String error) {
          setError(key, error);
        }
      }, ThreadGate.AWT);

      myDataProvider.setSingleData(TAG_EXPORTER_ROLE, tagExporter);
      myDataProvider.setSingleData(PROGRESS_MODIFIABLE_ROLE, progress.getModifiable());
      myDataProvider.setSingleData(SUCCESS_ROLE, false);
    }

    private File getWorkArea() throws CantPerformException {
      File workspaceDir = myWorkspaceChooser.getFile();
      if (!verifyWorkspace(workspaceDir)) throw new CantPerformException();
      return workspaceDir;
    }

    private File getOutputFile() throws CantPerformException {
      File outFile = CantPerformException.ensureNotNull(myOutputFileChooser.getFile());
      String error = verifyOutputFile(outFile);
      setError(TagExporterEnv.OUT_FILE_KEY, error);
      if (error != null) throw new CantPerformException();
      if (outFile.exists()) confirmOverwrite();
      return outFile;
    }

    /** @return null if output file is ok, displayable error message otherwise */
    @Nullable
    private String verifyOutputFile(File outFile) {
      try {
        if (outFile.getPath().isEmpty())
          return "Please specify a file";
        outFile = outFile.getCanonicalFile();
        if (outFile.isDirectory())
          return outFile + " is a folder";
        outFile.getParentFile().mkdirs();
        if (!outFile.getParentFile().isDirectory()) {
          return "Cannot create file";
        }
      } catch (IOException ex) {
        return "Cannot access file: " + ex.getMessage();
      }
      return null;
    }

    private void confirmOverwrite() throws CantPerformException {
      if (!DialogsUtil.askConfirmation(myMainPanel, L.content("File already exists. Overwrite?"), L.dialog("Confirm Export"))) {
        highlight(myOutputFileChooser.getField(), true);
        throw new CantPerformException();
      }
    }

    private void updateProgress(@NotNull TagExporter tagExporter) {
      Progress progress = tagExporter.getProgress();
      if (progress.isDone()) {
        if (tagExporter.isCancelled()) {
          myProgressBar.setValue(0);
          updateStatus();
        } else if (myErrors.isEmpty()) {
          reportSuccess();
        }
      } else {
        myProgressBar.setValue((int)(progress.getProgress()*100));
        updateStatus();
      }
    }

    private void reportSuccess() {
      myProgressBar.setValue(100);
      myErrors.clear();
      myDataProvider.setSingleData(SUCCESS_ROLE, true);
      updateStatus();
    }
  }

  private abstract class ShowResultAction extends SimpleAction {
    public ShowResultAction(String name) {
      super(name);
      watchRole(SUCCESS_ROLE);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(context.getSourceObject(SUCCESS_ROLE));
    }

    private File getOutputFile(ActionContext context) throws CantPerformException {
      return CantPerformException.ensureNotNull(context.getSourceObject(TAG_EXPORTER_ROLE).getOutputFile());
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      CantPerformException.ensure(context.getSourceObject(SUCCESS_ROLE));
      perform(getOutputFile(context), context.getComponent());
    }

    protected abstract void perform(File outputFile, Component owner);
  }

  private class ShowInFolderAction extends ShowResultAction {
    public ShowInFolderAction() {
      super(FileActions.canHighlightFiles()
        ? (Env.isMac() ? "&Show in Finder" : "&Show in Folder")
        : "Open Containing &Folder");
    }

    @Override
    protected void perform(File outputFile, Component owner) {
      FileActions.openContainingFolder(outputFile, owner);
    }
  }

  private class OpenFileAction extends ShowResultAction {
    public OpenFileAction() {
      super("&Open File");
    }

    @Override
    protected void perform(File outputFile, Component owner) {
      FileActions.openFile(outputFile, owner);
    }
  }
}