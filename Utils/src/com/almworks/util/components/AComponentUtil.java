package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author : Dyoma
 */
public class AComponentUtil {
  public static Detach selectElementWhenAny(FlatCollectionComponent component) {
    return selectElementWhenAny(component.getSelectionAccessor(), component.getCollectionModel());
  }

  public static void selectElementWhenAny(Lifespan life, FlatCollectionComponent<?> component) {
    SelectionAccessor<?> accessor = component.getSelectionAccessor();
    if (accessor.ensureSelectionExists()) return;
    if (life.isEnded()) return;
    life.add(selectElementWhenAny(accessor, component.getCollectionModel()));
  }

  private static Detach selectElementWhenAny(final SelectionAccessor accessor, AListModel collectionModel) {
    if (accessor.ensureSelectionExists())
      return Detach.NOTHING;
    return collectionModel.addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        accessor.ensureSelectionExists();
      }
    });
  }

  public static Detach selectNewElements(final FlatCollectionComponent component) {
    return component.getCollectionModel().addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        component.getSelectionAccessor().addSelectedRange(index, index + length - 1);
      }
    });
  }

  @Nullable
  public static <T> T getElementAtPoint(FlatCollectionComponent<? extends T> component, Point point) {
    int index = component.getElementIndexAt(point.x, point.y);
    return index >= 0 ? component.getCollectionModel().getAt(index) : null;
  }

  /**
   * Ensures that component has selection when model contains at least one element. Processes insert/remove events to
   * restore not empty selection if selected element goes away or model becomes not empty. 
   * @param life
   * @param component
   * @param <T>
   */
  public static <T> void keepNotEmptySelection(Lifespan life, final FlatCollectionComponent<T> component) {
    component.getSelectionAccessor().ensureSelectionExists();
    AListModel<T> model = (AListModel<T>) component.getCollectionModel();
    life.add(model.addListener(new AListModel.Adapter<T>() {
      @Override
      public void onInsert(int index, int length) {
        component.getSelectionAccessor().ensureSelectionExists();
      }

      @Override
      public void onRemove(int index, int length, AListModel.RemovedEvent<T> tRemovedEvent) {
        component.getSelectionAccessor().ensureSelectionExists();
      }
    }));
  }
}
