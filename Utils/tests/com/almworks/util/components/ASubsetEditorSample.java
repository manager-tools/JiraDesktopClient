package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Function;
import org.almworks.util.detach.Lifespan;

/**
 * @author : Dyoma
 */
public class ASubsetEditorSample {
  public static void main(String[] args) {
    OrderListModel<String> source = new OrderListModel<String>();
    for (int i = 0; i < 10; i++)
      source.addElement(String.valueOf(i));
    SubsetModel<String> model = SubsetModel.create(Lifespan.FOREVER, source, true);
    Function<String, String> stringFunction = new Function<String, String>() {
      public String invoke(String argument) {
        return argument;
      }
    };
    SubsetEditor<String> subsetEditor = SubsetEditor.create(model, true, null, Convertor.<String>identity(), stringFunction);
    LinkSample.showFrame(subsetEditor.getComponent(), "Subset editor");
  }
}
