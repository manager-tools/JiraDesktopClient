package com.almworks.search;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;

class SearchComponentUtils {
  public static String getStringPath(GenericNode node) {
    StringBuffer buffer = new StringBuffer();
    for (GenericNode n = node; n != null; n = n.getParent()) {
      if (n.isNarrowing() && !(n instanceof RootNode)) {
        if (buffer.length() > 0)
          buffer.insert(0, " : ");
        buffer.insert(0, n.getName());
      }
    }
    return buffer.length() > 0 ? buffer.toString() : "Everywhere";
  }
}
