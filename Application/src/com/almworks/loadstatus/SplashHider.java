package com.almworks.loadstatus;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.platform.about.SplashStage;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.properties.Role;
import org.almworks.util.detach.DetachComposite;
import org.picocontainer.Startable;
import util.concurrent.SynchronizedBoolean;

class SplashHider implements Startable {
  public static final Role<SplashHider> ROLE = Role.role(SplashHider.class);
  private final ApplicationLoadStatus myLoadStatus;

  private final SynchronizedBoolean myHidden = new SynchronizedBoolean(false);

  public SplashHider(ApplicationLoadStatus loadStatus) {
    myLoadStatus = loadStatus;
  }

  public void start() {
    final DetachComposite detach = new DetachComposite(true);
    ScalarModel<Boolean> model = myLoadStatus.getApplicationLoadedModel();
    model.getEventSource().addStraightListener(detach, new ScalarModel.Adapter<Boolean>() {
      public void onScalarChanged(ScalarModelEvent<Boolean> event) {
        Boolean value = event.getNewValue();
        if (value != null && value) {
          detach.detach();
          hideSplash();
        }
      }
    });
  }

  private void hideSplash() {
    if (myHidden.commit(false, true)) {
      SplashStage.perform(SplashStage::hide);
    }
  }

  public void stop() {
  }
}
