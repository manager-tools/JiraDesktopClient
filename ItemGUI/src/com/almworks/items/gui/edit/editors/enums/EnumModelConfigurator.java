package com.almworks.items.gui.edit.editors.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import java.util.HashSet;

public abstract class EnumModelConfigurator implements ChangeListener {
  private final UserDataHolder myData = new UserDataHolder();
  private final Lifecycle myLifecycle = new Lifecycle();
  private final EditItemModel myModel;
  private final VariantsAcceptor<ItemKey> myAcceptor;
  private ItemHypercube myPrevRestriction = null;

  public EnumModelConfigurator(EditItemModel model, VariantsAcceptor<ItemKey> acceptor) {
    myModel = model;
    myAcceptor = acceptor;
  }

  public void onChange() {
    HashSet<DBAttribute<Long>> attributes = Collections15.hashSet();
    collectCubeAttributes(attributes);
    ItemHypercube cube = myModel.collectHypercube(attributes);
    if (myPrevRestriction != null && myPrevRestriction.isSame(cube) && !isModelChanged(myModel, myData)) return;
    myPrevRestriction = cube;
    myLifecycle.cycle();
    Lifespan life = myLifecycle.lifespan();
    AListModel<LoadedItemKey> variants = getSortedVariantsModel(life, myModel, cube);
    updateVariants(life, myAcceptor, variants, myModel, myData);
  }

  protected boolean isModelChanged(EditItemModel model, UserDataHolder data) {
    return false;
  }

  public void start(Lifespan life) {
    life.add(myLifecycle.getDisposeDetach());
    myModel.addAWTChangeListener(life, this);
    onChange();
  }

  protected abstract void collectCubeAttributes(HashSet<DBAttribute<Long>> attributes);

  protected abstract void updateVariants(Lifespan life, VariantsAcceptor<ItemKey> acceptor, AListModel<LoadedItemKey> variants,
    EditItemModel model, UserDataHolder data);

  protected abstract AListModel<LoadedItemKey> getSortedVariantsModel(Lifespan life, EditItemModel model, ItemHypercube cube);

  public final VariantsAcceptor<ItemKey> getAcceptor() {
    return myAcceptor;
  }
}
