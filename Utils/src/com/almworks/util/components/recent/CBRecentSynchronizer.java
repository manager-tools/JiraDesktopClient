package com.almworks.util.components.recent;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.config.Configuration;
import org.almworks.util.detach.Lifespan;

public class CBRecentSynchronizer<T> {
  private final RecentController<T> myRecents;
  private final AComboboxModel<T> myOriginal;
  private final SelectionInListModel<T> mySync;

  private CBRecentSynchronizer(RecentController<T> recents, AComboboxModel<T> original, SelectionInListModel<T> sync) {
    myRecents = recents;
    myOriginal = original;
    mySync = sync;
  }

  public static <T> CBRecentSynchronizer<T> create(Lifespan life, AComboboxModel<T> original) {
    RecentController<T> recents = new RecentController<T>(original, true);
    recents.setInitial(original.getSelectedItem());
    SelectionInListModel<T> decorated =
      SelectionInListModel.create(life, recents.getDecoratedModel(), null);
    UnwrapCombo.selectRecent(decorated, original.getSelectedItem());
    CBRecentSynchronizer<T> synchronizer = new CBRecentSynchronizer<T>(recents, original, decorated);
    synchronizer.init(life);
    return synchronizer;
  }

  public static <T> void setupComboBox(Lifespan life, AComboBox<T> component, AComboboxModel<T> model, Configuration config, Convertor<? super T, String> convertor, CanvasRenderer<? super T> renderer) {
    CBRecentSynchronizer<T> sync = create(life, model);
    sync.setupRenderer(renderer);
    sync.setConfig(config, convertor);
    sync.copySelected();
    component.setCanvasRenderer(sync.getRenderer());
    component.setModel(sync.getModel());
    AddRecentFromComboBox.install(life, sync.myRecents, component);
  }

  public AComboboxModel<T> getModel() {
    return mySync;
  }

  public CanvasRenderer getRenderer() {
    return myRecents.getDecoratedRenderer();
  }

  public void setupRenderer(CanvasRenderer renderer) {
    myRecents.setRenderer(renderer);
  }

  public void setConfig(Configuration config, Convertor<? super T, String> convertor) {
    myRecents.setConfig(config);
    myRecents.setIdentityConvertor(convertor);
  }

  private void init(Lifespan life) {
    final boolean[] flag = { false };
    mySync.addSelectionChangeListener(life, new JointChangeListener(flag) {
      public void processChange() {
        myOriginal.setSelectedItem(RecentController.<T>unwrap(mySync.getSelectedItem()));
      }
    });
    myOriginal.addSelectionChangeListener(life, new JointChangeListener(flag) {
      public void processChange() {
        copySelected();
      }
    });
  }

  private void copySelected() {
    T selected = myOriginal.getSelectedItem();
    myRecents.setInitial(selected);
    UnwrapCombo.selectRecent(mySync, selected);
  }
}
