package com.almworks.actions.console.actionsource;

import com.almworks.actions.console.VariantModelController;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class ActionsModelFactory implements Function<Lifespan, VariantModelController<ActionEntry>> {
  private final List<Pair<ActionEntry, List<ActionEntry>>> myActions;

  ActionsModelFactory(List<Pair<ActionEntry, List<ActionEntry>>> actions) {
    myActions = actions;
  }

  @Override
  public VariantModelController<ActionEntry> invoke(Lifespan life) {
    return MyController.create(life, myActions);
  }

  private static class MyController implements VariantModelController<ActionEntry> {
    private final AListModel<ActionEntry> myWholeModel;
    private final List<FilteringListDecorator<ActionEntry>> mySegments;
    @Nullable
    private final FilteringListDecorator<ActionEntry> myDefaultSegment;

    private MyController(AListModel<ActionEntry> wholeModel, List<FilteringListDecorator<ActionEntry>> segments,
      @Nullable FilteringListDecorator<ActionEntry> defaultSegment) {
      myWholeModel = wholeModel;
      mySegments = segments;
      myDefaultSegment = defaultSegment;
    }

    public static MyController create(Lifespan life, List<Pair<ActionEntry, List<ActionEntry>>> groups) {
      List<FilteringListDecorator<ActionEntry>> segments = Collections15.arrayList();
      FilteringListDecorator<ActionEntry> defaultSegment = null;
      for (Pair<ActionEntry, List<ActionEntry>> pair : groups) {
        List<ActionEntry> actions = pair.getSecond();
        ActionEntry group = pair.getFirst();
        if (group.getDisplayName().isEmpty()) defaultSegment = FilteringListDecorator.create(life, FixedListModel.create(actions));
        else {
          ArrayList<ActionEntry> segmentList = Collections15.arrayList();
          segmentList.add(group);
          segmentList.addAll(actions);
          segments.add(FilteringListDecorator.create(life, FixedListModel.create(segmentList)));
        }
      }
      List<FilteringListDecorator<ActionEntry>> allSegments;
      if (defaultSegment == null) allSegments = segments;
      else {
        allSegments = Collections15.arrayList();
        allSegments.add(defaultSegment);
        allSegments.addAll(segments);
      }
      return new MyController(SegmentedListModel.create(life, allSegments), segments, defaultSegment);
    }

    @Override
    public void setText(String text) {
      NameFilter filter = new NameFilter(text);
      updateFilter(filter, myDefaultSegment);
      for (FilteringListDecorator<ActionEntry> segment : mySegments) {
        updateFilter(filter, segment);
      }
    }

    private void updateFilter(NameFilter filter, @Nullable FilteringListDecorator<ActionEntry> segment) {
      if (segment == null) return;
      boolean checkFirst = segment == myDefaultSegment;
      AListModel<ActionEntry> source = segment.getSource();
      boolean found = false;
      for (int i = (checkFirst ? 0 : 1); i < source.getSize(); i++) {
        ActionEntry action = source.getAt(i);
        if (filter.isAccepted(action)) {
          found = true;
          break;
        }
      }
      if (found) segment.setFilter(filter);
      else segment.setFilter(Condition.never());
    }

    @Override
    public AListModel<ActionEntry> getVariants() {
      return myWholeModel;
    }

  }

  private static class NameFilter extends Condition<ActionEntry> {
    private final String myExpectedPrefix;

    private NameFilter(String expectedPrefix) {
      myExpectedPrefix = Util.lower(expectedPrefix);
    }

    @Override
    public boolean isAccepted(ActionEntry value) {
      return value != null && (value.isGroup() || Util.lower(value.getDisplayName()).startsWith(myExpectedPrefix));
    }
  }
}
