package com.almworks.tags;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.FrameBuilder;
import com.almworks.api.gui.WindowManager;
import com.almworks.api.install.Setup;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.AActionButton;
import com.almworks.util.components.ALabel;
import com.almworks.util.components.FileSelectionField;
import com.almworks.util.i18n.Local;
import com.almworks.util.progress.Progress;
import com.almworks.util.tags.TagFileStorage;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import org.almworks.util.StringUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static javax.swing.SwingUtilities.isDescendingFrom;

public class ImportTagsAction extends SimpleAction {
  public ImportTagsAction() {
    super("&Import Tags\u2026");
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(true);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    FrameBuilder frameBuilder = context.getSourceObject(WindowManager.ROLE).createFrame("importTags");
    frameBuilder.setTitle(L.frame(Local.parse("Import " + Terms.ref_Deskzilla + " Tags")));
    ImportTagsForm form = new ImportTagsForm();
    frameBuilder.setContent(form.build());
    frameBuilder.setInitialFocusOwner(form.myFileSelector.getField());
    frameBuilder.setDefaultButton(form.myImportButton);
    frameBuilder.detachOnDispose(form.createCancelRunningImporterDetach());
    frameBuilder.setResizable(false);
    frameBuilder.setIgnoreStoredSize(true);
    frameBuilder.showWindow();
  }

  private static class ImportTagsForm {
    private static final String PROGRESS_CARD = "progress";
    private static final String ERROR_CARD = "error";
    private static final String DONE_CARD = "done";
    private static final TypedKey<File> FILE_DATA = TypedKey.create("file");
    private static final TypedKey<Progress> PROGRESS_DATA = TypedKey.create("progress");

    private final JPanel myMainPanel = new JPanel();
    private final FileSelectionField myFileSelector = new FileSelectionField();
    private final AActionButton myImportButton = new AActionButton();
    private final CardLayout myStatusLayout = new CardLayout();
    private final JPanel myStatusPanel = new JPanel(myStatusLayout);
    private Dimension myStatusSize = null;
    private final JProgressBar myProgressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
    private final ALabel myErrorLabel = new ALabel();
    private final ALabel myDoneLabel = new ALabel("Done");
    private final SimpleProvider myDataProvider = new SimpleProvider(FILE_DATA, PROGRESS_DATA);
    private TagsImporter myRunningImporter = null;

    public JPanel build() {
      setupComponents();
      setupLayout();
      setupListeners();
      setupDefaultValues();
      setupActions();
      return myMainPanel;
    }

    private void setupComponents() {
      myFileSelector.setDialogSelectButtonText("Select");
      myFileSelector.addExtensionFilter("Text files", "txt");
      myFileSelector.setDoubleClickEnabled(false);
      myErrorLabel.setForeground(GlobalColors.ERROR_COLOR);
    }

    private void setupLayout() {
      DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("d, 4dlu, fill:d:g"), myMainPanel);

      builder.append("1.");
      builder.append(Local.parse("Select a file containing tags exported from an earlier version of " + Terms.ref_Deskzilla + ":"));
      builder.append("");
      builder.append(myFileSelector);

      builder.append("2.");
      builder.append("<html>Click <b>Import</b> and wait a little.");

      builder.append("");
      JPanel importLinePanel = new JPanel(UIUtil.createBorderLayout());
      JPanel buttonPanel = new JPanel(new BorderLayout());
      buttonPanel.add(myImportButton, BorderLayout.NORTH);
      importLinePanel.add(buttonPanel, BorderLayout.WEST);
      myStatusPanel.add(myProgressBar, PROGRESS_CARD);
      myStatusPanel.add(myErrorLabel, ERROR_CARD);
      myStatusPanel.add(myDoneLabel, DONE_CARD);
      myStatusLayout.show(myStatusPanel, PROGRESS_CARD);
      importLinePanel.add(myStatusPanel, BorderLayout.CENTER);
      builder.append(importLinePanel);

      builder.append("3.");
      builder.append("<html>When import finishes, check the <b>Tags</b> folder in the Navigation Area.");

      myMainPanel.setBorder(Borders.DLU4_BORDER);
    }

    private void setupListeners() {
      myFileSelector.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          Component newOwner = e.getOppositeComponent();
          if (newOwner != null && isDescendingFrom(newOwner, myMainPanel)) {
            // focus switched from the text field somewhere in the same window: to prevent running when JFileChooser is opened
            try {
              File canonicalFile = new File(myFileSelector.getField().getText()).getCanonicalFile();
              myFileSelector.setFile(canonicalFile);
            } catch (IOException ex) {
              setFileError(ex.getMessage());
            }
          }
        }
      });
      myFileSelector.getFileChooser().addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
            myDataProvider.setSingleData(FILE_DATA, myFileSelector.getFileChooser().getSelectedFile());
          }
        }
      });
      myFileSelector.getField().getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void documentChanged(DocumentEvent e) {
          myDataProvider.setSingleData(FILE_DATA, myFileSelector.getFile());
        }
      });
    }

    private void setupDefaultValues() {
      myFileSelector.setFile(new File(Setup.getWorkspaceDir(), TagFileStorage.DEFAULT_FILE_NAME));
    }

    private void setupActions() {
      DataProvider.DATA_PROVIDER.putClientValue(myMainPanel, myDataProvider);
      myImportButton.setAnAction(new ImportAction());
    }

    private void setFileError(@Nullable String error) {
      myErrorLabel.setText(error);
      myStatusLayout.show(myStatusPanel, error == null ? PROGRESS_CARD : ERROR_CARD);
    }

    private void beforeStart() {
      myProgressBar.setIndeterminate(true);
      myProgressBar.setEnabled(true);
      myFileSelector.setEnabled(false);
      myStatusLayout.show(myStatusPanel, PROGRESS_CARD);
      if(myStatusSize == null) {
        myStatusSize = myStatusPanel.getPreferredSize();
      } else {
        myStatusPanel.setPreferredSize(myStatusSize);
      }
    }

    private void reportProgress(double progress) {
      myProgressBar.setValue((int)(progress*100));
      myProgressBar.setIndeterminate(false);
    }

    private void reportDone(List<String> errors) {
      myProgressBar.setValue(100);
      myProgressBar.setIndeterminate(false);
      myFileSelector.setEnabled(true);
      if(errors != null && !errors.isEmpty()) {
        if(errors.size() == 1) {
          myErrorLabel.setText(errors.get(0));
        } else {
          myErrorLabel.setText("<html>Errors during import:<br>" + StringUtil.implode(errors, "<br>"));
        }
        myStatusLayout.show(myStatusPanel, ERROR_CARD);
        myStatusPanel.setPreferredSize(null);
      } else {
        myStatusLayout.show(myStatusPanel, DONE_CARD);
        myStatusPanel.setPreferredSize(null);
      }
    }

    public Detach createCancelRunningImporterDetach() {
      return new Detach() {
        @Override
        protected void doDetach() throws Exception {
          if (myRunningImporter != null) {
            myRunningImporter.cancel();
          }
        }
      };
    }

    private class ImportAction extends SimpleAction {
      public ImportAction() {
        super("Import");
        watchRole(FILE_DATA);
        watchRole(PROGRESS_DATA);
      }

      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        if (myRunningImporter == null) {
          File file = context.getSourceObject(FILE_DATA);
          if (!file.exists()) {
            setFileError("File does not exist");
            context.setEnabled(false);
          } else if (!file.isFile()) {
            setFileError("Not a file");
            context.setEnabled(false);
          } else {
            setFileError(null);
          }
          myProgressBar.setValue(0);
        } else {
          Progress progress = ActionUtil.getNullable(context, PROGRESS_DATA);
          if (progress != null) {
            if (progress.isDone()) {
              reportDone(progress.getErrors(null));
              myRunningImporter = null;
            } else {
              reportProgress(progress.getProgress());
              context.setEnabled(false);
            }
            context.updateOnChange(progress.getModifiable());
          }
        }
      }

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        File file = context.getSourceObject(FILE_DATA);
        CantPerformException.ensure(file.isFile());
        Progress progress = new Progress("ITA");
        DialogManager diMan = context.getSourceObject(DialogManager.ROLE);
        TagsImporter importer = TagsImporter.create(file, progress, context, new ConfirmMergeTagDialog(diMan), new Procedure<String>() {
          @Override
          public void invoke(String arg) {
            setFileError(arg);
          }
        });
        beforeStart();
        myDataProvider.setSingleData(PROGRESS_DATA, progress);
        importer.run();
        myRunningImporter = importer;
      }
    }

    private static class ConfirmMergeTagDialog implements Function<String, Boolean> {
      private final DialogManager myDiMan;
      private final JCheckBox myRememberChoice = new JCheckBox(L.checkbox("Remember my choice"));

      @Nullable
      private Boolean myAlwaysMerge = null;
      private boolean myCurrentChoice = true;

      public ConfirmMergeTagDialog(DialogManager diMan) {
        myDiMan = diMan;
        myRememberChoice.setMnemonic('R');
      }

      @Override
      public Boolean invoke(String tagName) {
        if (myAlwaysMerge != null)
          return myAlwaysMerge;

        DialogBuilder builder = myDiMan.createBuilder("ImportTags.confirmMerge");
        builder.setTitle(L.dialog("Confirm Merging of Tags"));
        builder.setBottomBevel(false);
        builder.setModal(true);
        builder.setBottomLineComponent(myRememberChoice);
        ALabel label = new ALabel("<html>This workspace already contains tag <b>" + tagName + "</b>.<br> " +
          "Do you want to merge similar tag from other workspace or create a new tag with the same name and icon?</html>",
          UIManager.getIcon("OptionPane.informationIcon"), SwingConstants.LEADING);
        label.setPreferredWidth(40 * UIUtil.getColumnWidth(label));
        builder.setContent(label);
        builder.setResizable(false);
        builder.setOkAction(new SelectOptionAction(SelectOptionAction.MERGE_TYPE));
        builder.setCancelAction(new SelectOptionAction(SelectOptionAction.CREATE_NEW_TYPE));
        builder.showWindow();
        return myCurrentChoice;
      }

      private class SelectOptionAction extends EnabledAction {
        private static final int MERGE_TYPE = 0;
        private static final int CREATE_NEW_TYPE = 1;
        private final int myType;

        SelectOptionAction(int type) {
          super(L.actionName(type == MERGE_TYPE ? "&Merge" : "&Create New"));
          myType = type;
        }

        @Override
        protected void doPerform(ActionContext context) throws CantPerformException {
          myCurrentChoice = myType == MERGE_TYPE;
          if (myRememberChoice.isSelected()) {
            myAlwaysMerge = myCurrentChoice;
          }
        }
      }
    }
  }
}
