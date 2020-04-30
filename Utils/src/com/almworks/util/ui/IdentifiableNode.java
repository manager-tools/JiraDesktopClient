package com.almworks.util.ui;

import com.almworks.util.collections.Convertor;

/**
 * @author dyoma
 */
public interface IdentifiableNode {
  String getNodeId();

  Convertor<IdentifiableNode, String> GET_NODE_ID =new Convertor<IdentifiableNode, String>() {
    public String convert(IdentifiableNode value) {
      return value.getNodeId();
    }
  };
}
