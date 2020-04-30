package com.almworks.engine.gui;

import com.almworks.api.application.ModelMap;

import javax.swing.text.JTextComponent;

/**
 * Created by IntelliJ IDEA.
 * User: dyoma
 * Date: Apr 21, 2010
 * Time: 5:51:15 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TextValueController {
  void onModelChanged(ModelMap model, JTextComponent component);

  void onTextChanged(JTextComponent component, ModelMap model);
}
