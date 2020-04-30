package com.almworks.search;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.qb.*;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.ContainsTextConstraint;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.search.types.SearchWords;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.explorer.qbuilder.filter.TextAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.RemoveableModifiable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.TextUtil;
import com.almworks.util.text.parser.FormulaWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FullTextSearchDescriptor extends AbstractConstraintDescriptor implements CanvasRenderable{
  private final Engine myEngine;

  public FullTextSearchDescriptor(Engine engine) {
    myEngine = engine;
  }

  @Override
  public String getDisplayName() {
    return "Text Search";
  }

  @NotNull
  @Override
  public String getId() {
    return "$textSearch";
  }

  @Override
  public ConstraintEditor createEditor(ConstraintEditorNodeImpl node) {
    return TextAttribute.createEditor(node, false, "&Search string:",
      "<html>Enter search terms delimited by spaces.<br>Enclose exact phrases with \"double quotes.\"");
  }

  @Override
  public void writeFormula(FormulaWriter writer, PropertyMap data) {
    getType().writeFormula(writer, getId(), data);
  }

  @Override
  public AttributeConstraintType getType() {
    return TextAttribute.INSTANCE;
  }

  @NotNull
  @Override
  public ConstraintDescriptor resolve(NameResolver resolver, @Nullable ItemHypercube cube, @Nullable PropertyMap data) {
    return this;
  }

  @Override
  public RemoveableModifiable getModifiable() {
    return Modifiable.NEVER;
  }

  @Override
  public BoolExpr<DP> createFilter(PropertyMap data, ItemHypercube hypercube) {
    final List<String> stringList = TextAttribute.getSubstrings(data);
    if(stringList.isEmpty()) {
      return BoolExpr.TRUE();
    }
    final String[] words = stringList.toArray(new String[stringList.size()]);

    final Collection<Long> conns = ItemHypercubeUtils.getIncludedConnections(hypercube);
    if(conns != null && conns.size() == 1) {
      final Connection conn = findConnection(conns.iterator().next());
      return conn != null ? SearchWords.createFilter(conn, words) : BoolExpr.<DP>FALSE();
    }

    return new MyDP(words).term();
  }

  @Override
  public Constraint createConstraint(PropertyMap data, ItemHypercube cube) {
    List<String> stringList = TextAttribute.getSubstrings(data);
    if (stringList.isEmpty()) return Constraint.NO_CONSTRAINT;
    final String[] words = stringList.toArray(new String[stringList.size()]);
    return ContainsTextConstraint.Simple.create(words);
  }

  @Override
  public boolean isSameData(PropertyMap data1, PropertyMap data2) {
    return getType().isSameData(data1, data2);
  }

  @Override
  public CanvasRenderable getPresentation() {
    return this;
  }

  @Override
  public void renderOn(Canvas canvas, CellState state) {
    canvas.setIcon(getType().getDescriptorIcon());
    canvas.appendText("Text Search");
  }

  @Nullable
  private Connection findConnection(long connection) {
    return myEngine.getConnectionManager().findByItem(connection);
  }

  private class MyDP extends DP {
    private final String[] myWords;
    private final char[][] myChars;
    private final ConcurrentHashMap<Long, Connection> myConnections = new ConcurrentHashMap<Long, Connection>();

    public MyDP(String[] words) {
      myWords = words;
      myChars = SearchWords.wordsToCharArrays(words);
    }

    @Override
    public boolean accept(long item, DBReader reader) {
      if(item == 0) {
        return false;
      }

      final Long cid = reader.getValue(item, SyncAttributes.CONNECTION);
      if(cid == null || cid.longValue() == 0) {
        return false;
      }

      Connection conn = myConnections.get(cid);
      if(conn == null) {
        conn = findConnection(cid);
        if(conn != null) {
          myConnections.put(cid, conn);
        }
      }

      if(conn == null) {
        return false;
      }

      return conn.matchAllWords(item, myChars, myWords, reader);
    }

    @Override
    protected boolean equalDP(DP other) {
      return Arrays.equals(myWords, ((MyDP)other).myWords);
    }

    @Override
    protected int hashCodeDP() {
      return 41 + Arrays.hashCode(myWords);
    }

    @Override
    public String toString() {
      return "Text containsAny [" + TextUtil.separate(myWords, ", ") + ']';
    }
  }
}
