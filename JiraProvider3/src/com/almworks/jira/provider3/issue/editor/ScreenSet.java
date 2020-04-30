package com.almworks.jira.provider3.issue.editor;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.config.Configuration;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * Defines issue editor screens set (forms to edit an issue). Also it provides {@link #getScreenSelector(com.almworks.items.gui.edit.EditItemModel) ui component} to allow user
 * to choose desired screen (if such facility is implemented by the subclass).
 */
public interface ScreenSet {
  /**
   * Creates a screen set and remembers it in the model
   * @return all field editors which can appear on any screen from the set.<br>
   *   null if something goes wrong and screens are not initialized
   */
  @Nullable
  List<FieldEditor> install(VersionSource source, EditItemModel model);

  /**
   * @return component for right part of the editor's window. This component may allow user to switch screens<br>
   *   null means no additional component is provided/required by this implementation
   */
  @Nullable
  JComponent getScreenSelector(EditItemModel model);

  /**
   * Attaches and configures screen controller
   * @return true if controller is successfully attached. false means that problem has happened, edit is not possible
   */
  boolean attach(Lifespan life, ScreenController controller, Configuration config);
}
