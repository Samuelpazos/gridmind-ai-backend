package com.gridmind.command;

/**
 * Command Pattern Interface: Encapsulates actions as objects, supporting execution and undo.
 */
public interface GridCommand {
    String getCommandName();
    void execute();
    void undo();
    boolean isUndoable();
}
