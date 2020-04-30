package com.almworks.api.application;

import java.util.List;

/**
 * @author dyoma
 */
public interface ItemCollectorWidget {

  void loadingDone();

  void showLoadingMessage(String message);

  void showProgress(float percent);

  void showErrors(List<String> errors);

  ItemCollectorWidget DEAF = new  ItemCollectorWidget() {

    public void loadingDone() {
    }

    public void showLoadingMessage(String message) {
    }

    public void showProgress(float percent) {
    }

    public void showErrors(List<String> errors) {
    }

  };
}
