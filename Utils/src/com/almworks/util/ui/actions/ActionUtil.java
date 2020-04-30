package com.almworks.util.ui.actions;

import com.almworks.util.DECL;
import com.almworks.util.components.ACollectionComponent;
import com.almworks.util.components.CollectionCommandListener;
import com.almworks.util.components.NeighbourAwareSeparator;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.AActionComponent;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

/**
 * @author : Dyoma
 */
public class ActionUtil {
  public static PropertyMap createPresentation(String name) {
    PropertyMap result = new PropertyMap(null);
    result.put(PresentationKey.NAME, name);
    result.put(PresentationKey.ENABLE, EnableState.ENABLED);
    return result;
  }

  public static AnAction createAction(final PropertyMap presentation, final AnActionListener listener) {
    return new AnAction() {
      public void update(UpdateContext context) {
        context.setEnabled(EnableState.ENABLED);
        for (TypedKey<?> typedKey : presentation.keySet()) {
          if (typedKey instanceof PresentationKey<?>)
            copyPresentation(context, (PresentationKey<?>) typedKey, presentation);
        }
      }

      public void perform(ActionContext context) throws CantPerformException {
        listener.perform(context);
      }

      private <T> void copyPresentation(UpdateContext context, PresentationKey<T> typedKey, PropertyMap presentation) {
        context.putPresentationProperty(typedKey, presentation.get(typedKey));
      }
    };
  }

  public static AnAction createAction(AnActionListener operation, String name) {
    return createAction(createPresentation(name), operation);
  }

  public static boolean updateMenu(List<? extends Component> items) {
    boolean hasVisible = false;
    for (Component item : items) {
      if (item instanceof AActionComponent<?>) {
        //noinspection OverlyStrongTypeCast
        ((AActionComponent<?>) item).updateNow();
        hasVisible |= item.isVisible();
      }
    }
    for (Component component : items) {
      if (component instanceof JMenu) {
        boolean visible = updateMenu((JMenu) component);
        component.setVisible(visible);
        hasVisible |= visible;
      }
    }
    for (Component item : items) {
      if (item instanceof NeighbourAwareSeparator)
        ((NeighbourAwareSeparator) item).checkVisibility();
    }
    return hasVisible;
  }

  public static boolean updateMenu(JMenu menu) {
    return ActionUtil.updateMenu(Arrays.asList(menu.getMenuComponents()));
  }

  public static void performUpdate(@Nullable AnAction action, @NotNull UpdateContext context) {
    if (action == null)
      setNoAction(context);
    else
      try {
        action.update(context);
      } catch (CantPerformException e) {
        EnableState enableState = context.getPresentationProperty(PresentationKey.ENABLE);
        if (enableState != EnableState.DISABLED && enableState != EnableState.INVISIBLE)
          context.setEnabled(EnableState.DISABLED);
      }
  }

  public static void setNoAction(UpdateContext context) {
    context.setEnabled(EnableState.INVISIBLE);
    context.putPresentationProperty(PresentationKey.NOT_AVALIABLE, true);
  }

  public static boolean isActionEnabled(@Nullable AnAction action, @NotNull JComponent contextComponent) {
    if (action == null)
      return false;
    UpdateContext context = DefaultUpdateContext.singleUpdate(contextComponent);
    performUpdate(action, context);
    return context.getPresentationProperty(PresentationKey.ENABLE) == EnableState.ENABLED;
  }

  public static void performAction(@NotNull AnAction action, @NotNull JComponent contextComponent) {
    CantPerformExceptionExplained explained = performSafe(action, contextComponent);
    if (explained == null)
      return;
    UpdateContext context = DefaultUpdateContext.singleUpdate(contextComponent);
    performUpdate(action, context);
    String actionName = context.getPresentationProperty(PresentationKey.NAME);
    String title = actionName != null ? NameMnemonic.parseString(actionName).getText() : "Error";
    explained.explain(title, new DefaultActionContext(contextComponent));
  }

  @Nullable
  public static CantPerformExceptionExplained performSafe(@NotNull AnActionListener listener,
    @NotNull Component contextComponent)
  {
    return performSafe(listener, new DefaultActionContext(contextComponent));
  }

  public static <T> CollectionCommandListener<T> actionToCollectionCommand(final String actionId) {
    return new CollectionCommandListener<T>() {
      @Override
      public void onCollectionCommand(ACollectionComponent<T> component, int index, T element) {
        performSafe(new IdActionProxy(actionId), component.toComponent());
      }
    };
  }

  @Nullable
  public static CantPerformExceptionExplained performSafe(@NotNull AnActionListener listener,
    @NotNull ActionContext context)
  {
    try {
      listener.perform(context);
    } catch (CantPerformExceptionExplained explained) {
      return explained;
    } catch (CantPerformException e) {
      Log.warn("cannot perform action", e);
    }
    return null;
  }

  public static Detach setDefaultActionHandler(final AnActionListener defaultAction, final JComponent component) {
    DetachComposite life = new DetachComposite();
    UIUtil.addKeyListener(life, component, new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_ENTER) return;
        if ((e.getModifiersEx() & Shortcuts.KEYBOARD_MODIFIERS) != 0) return;
        performListener(defaultAction, component);
      }
    });
    UIUtil.addMouseListener(life, component, new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
          assert !e.isPopupTrigger(); /** @see com.almworks.util.ui.actions.presentation.BasePopupHandler#mouseClicked(java.awt.event.MouseEvent)  */
          DoubleClickAuthority authority =
            DoubleClickAuthority.PROPERTY.getClientValue(component);
          if (authority != null) {
            if (!authority.isDefaultActionAllowed(e, component)) {
              // skip action
              return;
            }
          }
          performListener(defaultAction, component);
        }
      }
    });
    return life;
  }

  private static void performListener(AnActionListener defaultAction, JComponent component) {
    try {
      defaultAction.perform(new DefaultActionContext(component));
    } catch (CantPerformException e1) {
      DECL.ignoreException();
    }
  }


  @Nullable
  public static <T> T getActor(Component component, TypedKey<? extends T> role) {
    try {
      return new DefaultActionContext(component).getSourceObject(role);
    } catch (CantPerformException e) {
      return null;
    }
  }

  @Nullable
  public static <T> T getNullable(ActionContext context, TypedKey<? extends T> role) {
    try {
      return context.getAvailableRoles().contains(role) ? context.getSourceObject(role) : null;
    } catch (CantPerformException e) {
      return null;
    }
  }
}
