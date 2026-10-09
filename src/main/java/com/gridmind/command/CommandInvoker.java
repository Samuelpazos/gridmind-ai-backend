package com.gridmind.command;

import com.gridmind.entity.CommandLogEntity;
import com.gridmind.repository.CommandLogRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

@Component
public class CommandInvoker {

    private final CommandLogRepository commandLogRepository;
    private final Deque<GridCommand> commandHistory = new ArrayDeque<>();

    public CommandInvoker(CommandLogRepository commandLogRepository) {
        this.commandLogRepository = commandLogRepository;
    }

    public void executeCommand(GridCommand command, String targetId, String parameters, String operator) {
        command.execute();

        if (command.isUndoable()) {
            commandHistory.push(command);
        }

        CommandLogEntity log = CommandLogEntity.builder()
                .commandName(command.getCommandName())
                .targetId(targetId)
                .parameters(parameters)
                .executedBy(operator != null ? operator : "OPERATOR")
                .undoable(command.isUndoable())
                .build();

        commandLogRepository.save(log);
    }

    public boolean undoLastCommand() {
        if (!commandHistory.isEmpty()) {
            GridCommand lastCommand = commandHistory.pop();
            lastCommand.undo();

            CommandLogEntity log = CommandLogEntity.builder()
                    .commandName("UNDO_" + lastCommand.getCommandName())
                    .targetId(null)
                    .parameters("Undo executed successfully")
                    .executedBy("SYSTEM_UNDO")
                    .undoable(false)
                    .build();

            commandLogRepository.save(log);
            return true;
        }
        return false;
    }
}
