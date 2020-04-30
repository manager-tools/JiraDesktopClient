package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.qb.FilterNode;
import com.almworks.util.text.parser.TokenRegistry;

public interface CustomParserProvider {
  void registerParsers(TokenRegistry<FilterNode> registry);
}
