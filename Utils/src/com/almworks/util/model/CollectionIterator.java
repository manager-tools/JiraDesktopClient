package com.almworks.util.model;


/**
 * :todoc:
 *
 * @author sereda
 */
public interface CollectionIterator <E> {
  ScalarModel<E> next();

  E last();

  boolean isFinished();
}

/*
class Example {
  void f() {
    final CollectionIterator<String> t = null;
    t.next().addListener(new ScalarModel.Consumer<String>() {
      public void scalarChanged(ScalarModel<String> scalarModel, String oldValue, String newValue) {
        final ScalarModel.Consumer me = this;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (t.isFinished())
              return;
            System.out.println(t.last());
            t.next().addListener(me);
          }
        });
      };
    });
  }
}
*/
