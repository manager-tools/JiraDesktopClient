package com.almworks.util.components;

import com.almworks.util.commons.Factory;
import com.almworks.util.commons.ProcedureE;
import com.almworks.util.files.ExternalBrowser;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.jetbrains.annotations.Nullable;

public class OpenBrowserAnAction extends SimpleAction {
  private final String myUrl;
  private final boolean myEncoded;
  private final ProcedureE<UpdateContext, CantPerformException> myUpdate;

  public OpenBrowserAnAction(String url, boolean encoded, String text) {
    this(url, encoded, text, null);
  }

  public OpenBrowserAnAction(String url, boolean encoded, String text, @Nullable ProcedureE<UpdateContext, CantPerformException> update) {
    super(text);
    myUrl = url;
    myEncoded = encoded;
    myUpdate = update;
  }

  public OpenBrowserAnAction(String url, boolean encoded, Factory<String> text, @Nullable ProcedureE<UpdateContext, CantPerformException> update) {
    super(text, null);
    myUrl = url;
    myEncoded = encoded;
    myUpdate = update;
  }


  protected void customUpdate(UpdateContext context) throws CantPerformException {
    if (myUpdate != null) myUpdate.invoke(context);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    ExternalBrowser.openURL(myUrl, myEncoded);
  }
}
