package com.almworks.api.application;

import com.almworks.util.properties.PropertyMap;

/**
 * @author dyoma
 */
public interface DataPromotionPolicy {
  <T> boolean canPromote(ModelKey<T> key, PropertyMap fromValues, PropertyMap toValues);

  DataPromotionPolicy STANDARD = new DataPromotionPolicy() {
    public <T> boolean canPromote(ModelKey<T> key, PropertyMap fromValues, PropertyMap toValues) {
      return key.isEqualValue(fromValues, toValues);
    }
  };

  DataPromotionPolicy ALWAYS = new DataPromotionPolicy() {
    public <T> boolean canPromote(ModelKey<T> key, PropertyMap fromValues, PropertyMap toValues) {
      return true;
    }
  };

  DataPromotionPolicy FULL_DOWNLOAD = new DataPromotionPolicy() {
    @Override
    public <T> boolean canPromote(ModelKey<T> key, PropertyMap fromValues, PropertyMap toValues) {
      ItemDownloadStage prev = ItemDownloadStageKey.retrieveValue(fromValues);
      if (prev == ItemDownloadStage.QUICK) {
        ItemDownloadStage next = ItemDownloadStageKey.retrieveValue(toValues);
        if (next.wasFull()) return true;
      }
      return STANDARD.canPromote(key, fromValues, toValues);
    }
  };

  /**
   * @deprecated actually not used
   */
  @Deprecated
  DataPromotionPolicy NEVER = new DataPromotionPolicy() {
    public <T> boolean canPromote(ModelKey<T> key, PropertyMap fromValues, PropertyMap toValues) {
      return false;
    }
  };
}
