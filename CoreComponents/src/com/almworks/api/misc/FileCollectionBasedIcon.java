package com.almworks.api.misc;

import com.almworks.util.exec.Context;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.IconSizeFixup;
import org.almworks.util.Log;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FileCollectionBasedIcon implements Icon {
  private static final long MINIMUM_PERIOD_BETWEEN_ATTEMPTS = 3000;

  private final String myCollectionName;
  private final String myIconPath;
  private final boolean myFixSize;

  private Icon myIcon;
  private long myLastLoadAttempt;

  public FileCollectionBasedIcon(String collectionName, String iconPath, boolean fixSize) {
    myCollectionName = collectionName;
    myIconPath = iconPath;
    myFixSize = fixSize;
  }

  private Icon loadIcon() {
    Threads.assertAWTThread();
    if (myIcon == null) {
      if (myLastLoadAttempt < System.currentTimeMillis() - MINIMUM_PERIOD_BETWEEN_ATTEMPTS) {
        WorkArea workArea = Context.get(WorkArea.class);
        if (workArea != null) {
          File file = workArea.getEtcCollectionFile(myCollectionName, myIconPath);
          if (file != null) {
            try {
              Icon icon = new ImageIcon(file.getAbsolutePath(), file.getName());
              if (myFixSize)
                icon = new IconSizeFixup(icon, 16, 16);
              myIcon = icon; 
            } catch (Exception e) {
              Log.debug(this + ": cannot load");
            }
          }
        }
        myLastLoadAttempt = System.currentTimeMillis();
      }
    }
    return myIcon;
  }

  public String getIconPath() {
    return myIconPath;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    Icon icon = loadIcon();
    if (icon != null) {
      icon.paintIcon(c, g, x, y);
    }
  }

  public int getIconWidth() {
    Icon icon = loadIcon();
    return icon == null ? -1 : icon.getIconWidth();
  }

  public int getIconHeight() {
    Icon icon = loadIcon();
    return icon == null ? -1 : icon.getIconHeight();
  }

  public String toString() {
    return "FCBI[" + myIconPath + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    FileCollectionBasedIcon other = Util.castNullable(FileCollectionBasedIcon.class, obj);
    return other != null && Util.equals(myCollectionName, other.myCollectionName) && Util.equals(myIconPath, other.myIconPath) && myFixSize == other.myFixSize;
  }

  @Override
  public int hashCode() {
    return myIconPath.hashCode() ^ myCollectionName.hashCode();
  }
}
