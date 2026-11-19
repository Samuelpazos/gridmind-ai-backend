package com.gridmind.command;

import java.util.Map;

/**
 * Command Pattern Interface: Encapsulates actions as objects, supporting execution and undo.
 */
public interface GridCommand {
    String getCommandName();
    void execute();
    Map<String, Object> createMemento();
    void undo();
    void undo(Map<String, Object> memento);
    boolean isUndoable();
}
