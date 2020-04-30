package com.almworks.export;

import com.almworks.util.progress.Progress;
import com.almworks.util.ui.actions.*;
import util.concurrent.SynchronizedBoolean;

public class CancelExportAction extends SimpleAction {
  private final Progress myProgress;
  private final SynchronizedBoolean myCancelled;

  public CancelExportAction(Progress progress, SynchronizedBoolean cancelled) {
    super("Cancel");
    myProgress = progress;
    myCancelled = cancelled;
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.putPresentationProperty(PresentationKey.NAME, myProgress.isDone() ? "Close" : "Cancel");
    context.updateOnChange(myProgress.getModifiable());
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    myCancelled.set(true);
  }
}
