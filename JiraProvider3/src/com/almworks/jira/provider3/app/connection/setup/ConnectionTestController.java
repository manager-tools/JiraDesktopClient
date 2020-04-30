package com.almworks.jira.provider3.app.connection.setup;

import com.almworks.api.container.ComponentContainer;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasSection;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.ColorUtil;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * @author dyoma
 */
class ConnectionTestController {
  private static final Factory<String> MESSAGE_TEST_FAILED = JiraConnectionWizard.LOCAL.getFactory("wizard.message.testfailed");

  private final JiraConnectionWizard myWizard;
  private final Lifecycle myTestLife = new Lifecycle();
  private final ComponentContainer myContainer;

  @ThreadAWT
  private ServerConnectionInfo myServerInfo = null;

  public ConnectionTestController(JiraConnectionWizard wizard, ComponentContainer container) {
    myWizard = wizard;
    myContainer = container;
  }

  private void doTest() {
    myTestLife.cycle();
    myServerInfo = null;
    final Lifespan lifespan = myTestLife.lifespan();
    new TestRunner(lifespan, myWizard.prepareTestConnection()).start();
  }

  public ServerConnectionInfo getServerInfo() {
    return myServerInfo;
  }

  public void testConnectionNow() {
    stopTest();
    doTest();
  }

  public void stopTest() {
    myTestLife.cycle();
  }

  public static final Convertor<Project, String> STRING_ID = new Convertor<Project, String>() {
    @Override
    public String convert(Project value) {
      return String.valueOf(value.getId());
    }
  };

  public void resetServerInfo() {
    myServerInfo = null;
  }

  public static class Project implements CanvasRenderable {
    public static final Comparator<Project> COMPARATOR = new Comparator<Project>() {
      @Override
      public int compare(Project p1, Project p2) {
        if (p1 == p2)
          return 0;
        if (p1 == null)
          return -1;
        if (p2 == null)
          return 1;
        return String.CASE_INSENSITIVE_ORDER.compare(p1.getDisplayName(), p2.getDisplayName());
      }
    };
    private final String myName;
    private final String myKey;
    private final int myId;

    public Project(int id, String name, String key) {
      myId = id;
      myName = name;
      myKey = key;
    }

    public void renderOn(Canvas canvas, CellState state) {
      canvas.appendText(myName);
      if(myKey != null && !myKey.isEmpty()) {
        final CanvasSection sec = canvas.newSection();
        sec.setForeground(ColorUtil.between(state.getOpaqueBackground(), state.getForeground(), 0.5f));
        sec.appendText(" (" + myKey + ")");
      }
    }

    public String toString() {
      return myName;
    }

    public String getDisplayName() {
      return myName;
    }

    public int getId() {
      return myId;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
      if(this == o) {
        return true;
      }

      if(o == null || getClass() != o.getClass()) {
        return false;
      }

      final Project project = (Project) o;

      if(myId != project.myId) {
        return false;
      }

      if(myName != null ? !myName.equals(project.myName) : project.myName != null) {
        return false;
      }

      return true;
    }

    public int hashCode() {
      int result;
      result = (myName != null ? myName.hashCode() : 0);
      result = 31 * result + myId;
      return result;
    }
  }

  private class TestRunner implements ConnectionChecker.Callback, Runnable {
    private final Lifespan myLifespan;
    private final ConnectionChecker myLoader;

    public TestRunner(Lifespan lifespan, ServerConfig serverConfig) {
      myLifespan = lifespan;
      myLoader = new ConnectionChecker(myContainer, this, serverConfig);
    }

    @Override
    public void addMessage(final boolean problem, @NotNull final String shortMessage, @Nullable final String longHtml) {
      if (myLifespan.isEnded()) return;
      ThreadGate.AWT.execute(() -> {
        if (!myLifespan.isEnded()) myWizard.showInfo(problem, shortMessage, longHtml);
      });
    }

    public void start() {
      myWizard.clearMessages();
      myWizard.showTesting(true);
      myLifespan.add(new Detach() {
        protected void doDetach() throws Exception {
          myWizard.showTesting(false);
          myLoader.cancel();
        }
      });
      ThreadGate.LONG(ConnectionChecker.class).execute(this);
    }

    @Override
    public void run() {
      ServerConnectionInfo info = null;
      try {
        info = myLoader.checkConnection();
      } finally {
        if (!myLifespan.isEnded()) {
          final ServerConnectionInfo finalInfo = info;
          ThreadGate.AWT.execute(() -> finishTest(finalInfo));
        }
      }
    }

    @ThreadAWT
    private void finishTest(ServerConnectionInfo info) {
      myServerInfo = info;
      myWizard.showTesting(false);
      if (info != null) {
        myWizard.updateUrl(info.getBaseUrl());
      }
      if (info != null && info.isConnectionAllowed()) myWizard.showSuccessful();
      else myWizard.showInfo(true, MESSAGE_TEST_FAILED.create(), null);
    }
  }
}
