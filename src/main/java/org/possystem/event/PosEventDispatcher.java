package org.possystem.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface and default implementation for dispatching POS events.
 * Any class that needs to fire POS events must implement this interface.
 * Example implementors: TransactionService, PriceBookService
 */
public interface PosEventDispatcher {

    List<PosEventListener> listeners = new ArrayList<>();

    default void addListener(PosEventListener listener) {
        listeners.add(listener);
    }

    default void removeListener(PosEventListener listener) {
        listeners.remove(listener);
    }

    default void dispatchEvent(PosEvent event, Object data) {
        for (PosEventListener listener : listeners) {
            listener.onEvent(event, data);
        }
    }
}