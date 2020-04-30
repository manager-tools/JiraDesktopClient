package com.almworks.spi.provider;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionState;
import com.almworks.api.engine.ConnectionViews;
import com.almworks.api.engine.InitializationState;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.platform.ProductInformation;
import com.almworks.items.api.DBEvent;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBLiveQuery;
import com.almworks.items.api.DBReader;
import com.almworks.util.AppBook;
import com.almworks.util.English;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.ALabel;
import com.almworks.util.components.Link;
import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.components.URLLink;
import com.almworks.util.components.plaf.LinkUI;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileActions;
import com.almworks.util.i18n.LText2;
import com.almworks.util.i18n.Local;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.StringUtil;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.Collection;

public abstract class DefaultInformationPanel implements UIComponentWrapper {
  private static final String PREFIX = "InformationPanel.";
  private static final LText2<Long, Long> REPORT = AppBook.text(PREFIX + "REPORT",
    "<html><body>{0,number,######} total<br>" +
    "{1,number,######} modified", 0L, 0L);

  private final DetachComposite myDetach = new DetachComposite();

  private final JScrollPane myWholePanel = new JScrollPane() {
    public void addNotify() {
      super.addNotify();
      attach();
    }
  };

  protected Form myForm;
  protected final Connection myConnection;
  private final ScalarModel<?> myConfigurationModel;

  public DefaultInformationPanel(Connection connection, ScalarModel<?> configurationModel) {
    myConnection = connection;
    myConfigurationModel = configurationModel;
  }

  public Connection getConnection() {
    return myConnection;
  }

  protected void setupForm() {
    WorkArea workArea = myConnection.getContext().getContainer().getActor(WorkArea.APPLICATION_WORK_AREA);
    File workspaceDir = workArea != null ? workArea.getRootDir() : null;
    myForm = new Form(workspaceDir);
    JPanel p = myForm.myFormPanel;
    p.setAlignmentX(0F);
    p.setAlignmentY(0F);
    p.setBorder(new EmptyBorder(19, 19, 19, 19));
    ScrollablePanel scrollable = new ScrollablePanel(p);
    new DocumentFormAugmentor().augmentForm(myDetach, scrollable, true);
    myWholePanel.setViewportView(scrollable);
    Aqua.setLightNorthBorder(myWholePanel);
    Aero.cleanScrollPaneBorder(myWholePanel);
  }

  protected void attach() {
    myForm.calculateStats();
    final Modifiable modifiable = getConnectionModifiable(myDetach);
    modifiable.addAWTChangeListener(myDetach, new ChangeListener() {
      @Override
      public void onChange() {
        myForm.updateInfo(getConnectionInfo());
      }
    });
     myForm.updateInfo(getConnectionInfo());
  }

  public void dispose() {
    myDetach.detach();
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  protected Modifiable getConnectionModifiable(Lifespan life) {
    final SimpleModifiable modifiable = new SimpleModifiable();
    ((ScalarModel<Object>) myConfigurationModel).getEventSource().addAWTListener(life, new ScalarModel.Adapter<Object>() {
      @Override
      public void onScalarChanged(ScalarModelEvent<Object> event) {
        modifiable.fireChanged();
      }
    });
    return modifiable;
  }

  protected abstract ConnectionInfo getConnectionInfo();

  protected static UIComponentWrapper getLazyWrapper(
    final ComponentContainer container,
    final Class<? extends DefaultInformationPanel> clazz)
  {
    return new LazyWrapper() {
      protected UIComponentWrapper initialize() {
        return container.instantiate(clazz);
      }
    };
  }

  protected String extractStatus() {
    ProductInformation pi = Context.require(ProductInformation.class);
    return "";
//    final ConnectionSyncMode syncMode = myConnection.getContext().getSyncMode();
//    if(syncMode.isSyncDenied()) {
//      return "The connection is offline, synchronization is disabled.";
//    } else if(Boolean.TRUE.equals(syncMode.isLightMode())) {
//      return "The connection is subject to " + pi.getName() + " limitations.";
//    } else if(syncMode.isUploadDenied()) {
//      return Local.parse("The connection is read-only, no changes to " + Terms.ref_artifacts + " are allowed.");
//    } else {
//      return "The connection is online.";
//    }
  }

  class Form {
    private JLabel myStateValue;
    private JPanel myFormPanel;
    private JLabel myBugsLabel;
    private JLabel myProductsLabel;
    private JLabel myLoginLabel;
    private ALabel myNameLabel;
    private URLLink myUrlLink;
    private JLabel myLoginValue;
    private ALabel myBugsValue;
    private ALabel myProductsValue;
    private JLabel myStatusValue;
    private Link myWorkspaceUrl;

    public Form(final File workspaceDir) {
      setupFonts();
      setupAlignments();
      listenConnection();
      if (workspaceDir == null) myWorkspaceUrl.setVisible(false);
      else {
        myWorkspaceUrl.setDisabledLook(LinkUI.NormalPaint.createDefault());
        myWorkspaceUrl.setPresentationMapping(Action.SHORT_DESCRIPTION, PresentationMapping.GET_SHORT_DESCRIPTION);
        myWorkspaceUrl.setAnAction(new SimpleAction() {
          @Override
          protected void customUpdate(UpdateContext context) throws CantPerformException {
            context.setEnabled(FileActions.isSupported(FileActions.Action.OPEN_CONTAINING_FOLDER));
            context.putPresentationProperty(PresentationKey.NAME, workspaceDir.getAbsolutePath());
          }

          @Override
          protected void doPerform(ActionContext context) throws CantPerformException {
            FileActions.openContainingFolder(workspaceDir, context.getComponent());
          }
        });
      }
    }

    public void updateInfo(ConnectionInfo info) {
      myNameLabel.setText(info.connectionName);
      myUrlLink.setUrl(info.connectionUrl, false);
      myStatusValue.setText(info.status);
      myLoginLabel.setText(info.loginName);
      myLoginValue.setText(info.loginValue);
      if (info.productsName != null) {
        myProductsLabel.setText(info.productsName);
        myProductsValue.setText("<html>" + StringUtil.implode(info.productsValue, "<br>"));
      }
      myBugsLabel.setText(Local.text(Terms.key_Artifacts) + ":");
    }

    public void calculateStats() {
      final ConnectionViews views = myConnection.getViews();
      final DBFilter total = views.getConnectionItems();
      final DBFilter changed = views.getOutbox();

      final DBLiveQuery.Listener updater = new DBLiveQuery.Listener() {
        @Override
        public void onICNPassed(long icn) {
        }

        @Override
        public void onDatabaseChanged(DBEvent event, DBReader reader) {
          final long totalCount = total.query(reader).count();
          final long changedCount = changed.query(reader).count();
          ThreadGate.AWT.execute(new Runnable() {
            @Override
            public void run() {
              myBugsValue.setText(Local.parse(REPORT.format(totalCount, changedCount)));
            }
          });
        }
      };

      total.liveQuery(myDetach, updater);
      changed.liveQuery(myDetach, updater);
    }

    private void listenConnection() {
      final ScalarModel.Consumer updater = new ScalarModel.Adapter() {
        @Override
        public void onScalarChanged(ScalarModelEvent objectScalarModelEvent) {
          final ConnectionState cState = myConnection.getState().getValue();
          final InitializationState iState = myConnection.getInitializationState().getValue();
          myStateValue.setText(getStateText(cState, iState));
        }
      };
      myConnection.getState().getEventSource().addAWTListener(myDetach, updater);
      myConnection.getInitializationState().getEventSource().addAWTListener(myDetach, updater);
    }

    private String getStateText(ConnectionState cState, InitializationState iState) {
      final String text;
      if (cState == null || iState == null) {
        text = "";
      } else if (cState.isDegrading()) {
        text = cState.getName();
      } else if (!iState.isInitialized()) {
        text = iState.getName();
      } else if (iState == InitializationState.REINITIALIZING) {
        text = iState.getName();
      } else if (iState == InitializationState.REINITIALIZATION_REQUIRED && cState == ConnectionState.READY) {
        text = cState.getName() + ", " + iState;
      } else {
        text = cState.getName();
      }
      return English.humanizeEnumerable(text);
    }

    private void setupFonts() {
      myNameLabel.setBorder(UIUtil.createSouthBevel(AwtUtil.getPanelBackground()));
      UIUtil.adjustFont(myNameLabel, 1.35F, Font.BOLD, true);
    }

    private void setupAlignments() {
      myUrlLink.setAlignmentY(0f);
      myWorkspaceUrl.setHorizontalAlignment(SwingConstants.LEADING);
      protect(myNameLabel, myStatusValue, myLoginValue, myProductsValue, myBugsValue, myStateValue, myWorkspaceUrl);
      UIUtil.setDefaultLabelAlignment(myFormPanel);
    }

    private void protect(JComponent... labels) {
      for(final JComponent c : labels) {
        c.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
      }
    }
  }

  protected static class ConnectionInfo {
    public final String connectionName;
    public final String connectionUrl;
    public final String status;
    public final String loginName;
    public final String loginValue;
    public final String productsName;
    public final Collection<String> productsValue;

    public ConnectionInfo(
      String connectionName, String connectionUrl, String status,
      String loginName, String loginValue,
      String productsName, Collection<String> productsValue)
    {
      this.connectionName = connectionName;
      this.connectionUrl = connectionUrl;
      this.status = status;
      this.loginName = loginName;
      this.loginValue = loginValue;
      this.productsName = productsName;
      this.productsValue = productsValue;
    }
  }
}
