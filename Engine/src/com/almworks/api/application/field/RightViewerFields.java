package com.almworks.api.application.field;

import com.almworks.api.application.ModelMap;
import com.almworks.api.application.ViewerFieldsManager;
import com.almworks.api.application.viewer.UIController;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.Highlightable;
import com.almworks.util.components.layout.WidthDrivenColumn;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.DocumentFormAugmentor;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

/**
 * @author dyoma
 */
public class RightViewerFields implements UIController<WidthDrivenColumn> {
  private final Configuration mySettings;
  private final ViewerFieldsManager myViewerFields;
  private final DocumentFormAugmentor myDocumentFormAugmentor;

  private final Procedure<Highlightable> myDetach;
  private final Procedure<Highlightable> myAttach;

  private RightViewerFields(Configuration settings, ViewerFieldsManager viewerFields, DocumentFormAugmentor documentFormAugmentor, Procedure<Highlightable> attach,
    Procedure<Highlightable> detach)
  {
    mySettings = settings;
    myViewerFields = viewerFields;
    myDocumentFormAugmentor = documentFormAugmentor;
    myAttach = attach;
    myDetach = detach;
  }

  public static WidthDrivenComponent createComponent(Configuration settings, ViewerFieldsManager viewerFields, DocumentFormAugmentor documentFormAugmentor,
    Procedure<Highlightable> attaHighlightableProcedure, Procedure<Highlightable> detachAllHi)
  {
    WidthDrivenColumn result = new WidthDrivenColumn();
    CONTROLLER.putClientValue(result,
      new RightViewerFields(settings, viewerFields, documentFormAugmentor, attaHighlightableProcedure, detachAllHi));
    return result;
  }

  public void connectUI(@NotNull final Lifespan lifespan, @NotNull final ModelMap model,
    @NotNull final WidthDrivenColumn component)
  {
    if (lifespan.isEnded()) return;
    myViewerFields.addRightFields(this, component, lifespan, model, mySettings);
  }

  public DocumentFormAugmentor getDocumentFormAugmentor() {
    return myDocumentFormAugmentor;
  }

  public void setupHighlighting(final Highlightable highlightable, Lifespan life) {
    myAttach.invoke(highlightable);
    life.add(new Detach() {
      protected void doDetach() throws Exception {
        myDetach.invoke(highlightable);
      }
    });
  }
}
