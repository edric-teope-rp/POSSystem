package org.possystem.event;

/**
 * Interface for classes that listen to POS events.
 * Any class that needs to react to POS events
 * must implement this interface.
 * Example implementors: Swing UI, Virtual Journal (Phase 2)
 */
public interface PosEventListener {
    void onEvent(PosEvent event, Object data);
}