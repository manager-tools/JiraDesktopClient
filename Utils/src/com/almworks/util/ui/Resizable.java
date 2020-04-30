package com.almworks.util.ui;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface Resizable {
  int getWidth();

  int getHeight();

  void setSize(int width, int height);

  int addWidth(int plusWidth);

  int addHeight(int plusHeight);
}
