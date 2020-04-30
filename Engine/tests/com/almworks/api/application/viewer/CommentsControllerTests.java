package com.almworks.api.application.viewer;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;

import java.util.Collections;
import java.util.List;

public class CommentsControllerTests extends GUITestCase {
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  public void testUpdateModel() {
    OrderListModel<CommentState<MockComment>> model = new OrderListModel<CommentState<MockComment>>();
    List<CommentState<MockComment>> states = MockComment.createStateList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
    CommentsController.updateModel(states, model);
    assertEquals(11, model.getSize());
    for (int i = 0; i < 10; i++) assertFalse(model.getAt(i).isCollapsed());
    assertTrue(model.getAt(10).isCollapsed());

    states.add(MockComment.createState("12"));
    CommentsController.updateModel(states, model);
    for (int i = 0; i < 10; i++) assertFalse(model.getAt(i).isCollapsed());
    assertTrue(model.getAt(10).isCollapsed());
    assertFalse(model.getAt(11).isCollapsed());

    states.add(2, MockComment.createState("3a"));
    CommentsController.updateModel(states, model);
    for (int i = 0; i < 11; i++) assertFalse(model.getAt(i).isCollapsed());
    assertTrue(model.getAt(11).isCollapsed());
    assertFalse(model.getAt(12).isCollapsed());

    states.set(1, MockComment.createState("2a"));
    states.set(11, MockComment.createState("11a"));
    CommentsController.updateModel(states, model);
    for (int i = 0; i < 11; i++) assertFalse(model.getAt(i).isCollapsed());
    assertTrue(model.getAt(11).isCollapsed());
    assertFalse(model.getAt(12).isCollapsed());

    CHECK.order(new String[]{"1", "2a", "3a", "3", "4", "5", "6", "7", "8", "9", "10", "11a", "12"}, 
      MockComment.collectTexts(model.toList()));

    Collections.swap(states, 1, 11);
    CommentsController.updateModel(states, model);
    CHECK.order(new String[]{"1", "11a", "3a", "3", "4", "5", "6", "7", "8", "9", "10", "2a", "12"},
      MockComment.collectTexts(model.toList()));
    for (int i = 0; i < model.getSize(); i++) assertFalse(model.getAt(i).isCollapsed());
  }
}
