package com.almworks.api.application;

import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.properties.Role;
import org.almworks.util.detach.Lifespan;

public interface ApplicationLoadStatus {
  Role<ApplicationLoadStatus> ROLE = Role.role(ApplicationLoadStatus.class);

  /**
   * @return model that gets set to true when application is loaded
   * (whatever that means)
   */
  ScalarModel<Boolean> getApplicationLoadedModel();

  /**
   * This method can be called during dependent service construction.
   */
  StartupActivity createActivity(String debugName);



  abstract class StartupActivity {
    private final String myDebugName;

    public StartupActivity(String debugName) {
      myDebugName = debugName;
    }

    /**
     * Notify that the activity is done
     */
    public abstract void done();

    /**
     * Lifespan ends when this activity is {@link #done() done} or when startup watching is terminated for other reason
     */
    public abstract Lifespan getLife();

    /**
     * Create sub activity.<br>
     * This allows to create composite startup activity:<br>
     * 1. Parent activity registers during it own creation<br>
     * 2. During own startup parent activity creates children startup processes and creates {@link StartupActivity activities} for each of them<br>
     * 3. Parent activity ends own startup and {@link #done() notifies} that it is done.<br>
     */
    public abstract StartupActivity createSubActivity(String debugName);

    public final String getDebugName() {
      return myDebugName;
    }

    @Override
    public String toString() {
      return String.format("Startup activity '%s'", myDebugName);
    }

    public final void doneOn(ScalarModel<Boolean> readyModel) {
      readyModel.getEventSource().addStraightListener(getLife(), new ScalarModel.Adapter<Boolean>() {
        @Override
        public void onScalarChanged(ScalarModelEvent<Boolean> event) {
          if (event.getNewValue()) done();
        }
      });
    }
  }
}
