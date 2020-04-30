package com.almworks.gui;

import com.almworks.util.ui.actions.AnAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Wizard page interface.
 */
public interface WizardPage {
  /**
   * @return The component that is this page's main content.
   */
  @NotNull
  JComponent getContent();

  /**
   * Returns this page's {@code String} identifier, which is used by
   * other pages to refer to this page. This method is called once
   * by the wizard; the value it returns must not change.
   * @return This page ID.
   */
  @NotNull
  String getPageID();

  /**
   * Returns the ID of the next page. Can return {@link Wizard#FINISH_ID}
   * to indicate that this is the last page of the wizard.
   * This method is called once before the page is shown to check for
   * {@code FINISH_ID}. Then it is called when the "Next" button is pressed
   * to find out the actual page ID.
   * @return Next page ID.
   */
  @NotNull
  String getNextPageID();

  /**
   * Returns the ID of the previous page. Can return {@link Wizard#CANCEL_ID}
   * to indicate that this is the first page of the wizard.
   * This method is called once before the page is shown to check for
   * {@code CANCEL_ID}. Then it is called when the "Back" button is pressed
   * to find out the actual page ID.
   * @return Previous page ID.
   */
  @NotNull
  String getPrevPageID();

  /**
   * This method should return an array of additional actions, like
   * an "Advanced" action, if there are any. Otherwise, it is safe
   * to return {@code null}.
   * @return Additional actions.
   */
  @Nullable
  AnAction[] getMoreActions();

  /**
   * This method gets called when the wizard is about to show
   * this page, but hasn't shown it yet.
   * This is where a page can read some wizard state and
   * initialize its controls.
   * @param prevPageID The ID of the page that is being replaced by this page.
   */
  void aboutToDisplayPage(@Nullable String prevPageID);

  /**
   * This method gets called when the wizard has just shown this page.
   * This is where a page can attach listeners or launch a background
   * process.
   * @param prevPageID The ID of the page that is being replaced by this page.
   */
  void displayingPage(@Nullable String prevPageID);

  /**
   * This method gets called when the wizard is about to hide this page.
   * This is where a page can remove its listeners and add its values
   * to the wizard state.
   * @param nextPageID The ID of the page that is replacing by this page.
   */
  void aboutToHidePage(@NotNull String nextPageID);

  /**
   * This method gets called when the user has invoked the "Next" action.
   * This is the last moment for a page to determine its next page ID.
   * {@link #getNextPageID()} is called after this method.
   */
  void nextInvoked();

  /**
   * This method gets called when the user has invoked the "Back" action.
   * This is the last moment for a page to determine its previous page ID.
   * {@link #getPrevPageID()} is called after this method.
   */
  void backInvoked();
}
