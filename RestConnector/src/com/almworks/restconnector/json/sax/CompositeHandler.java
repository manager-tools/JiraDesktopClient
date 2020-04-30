package com.almworks.restconnector.json.sax;

import com.almworks.util.text.TextUtil;
import org.almworks.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Arrays;

public class CompositeHandler implements LocationHandler {
  private final LocationHandler[] myHandlers;

  public CompositeHandler(LocationHandler ... handlers) {
    myHandlers = ArrayUtil.arrayCopy(handlers);
  }

  @Override
  public void visit(Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
    for (LocationHandler handler : myHandlers) handler.visit(what, start, key, value);
  }

  @Override
  public String toString() {
    return "{" + TextUtil.separateToString(Arrays.asList(myHandlers), "; ");
  }
}
