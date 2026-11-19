package com.gridmind.command;

import com.gridmind.entity.CommandLogEntity;
import com.gridmind.entity.CommandSnapshotEntity;
import com.gridmind.repository.CommandLogRepository;
import com.gridmind.repository.CommandSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Component
public class CommandInvoker {

    private final CommandLogRepository commandLogRepository;
    private final CommandSnapshotRepository commandSnapshotRepository;
    private final ObjectMapper objectMapper;

    public CommandInvoker(CommandLogRepository commandLogRepository,
                          CommandSnapshotRepository commandSnapshotRepository,
                          ObjectMapper objectMapper) {
        this.commandLogRepository = commandLogRepository;
        this.commandSnapshotRepository = commandSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    public void executeCommand(GridCommand command, String targetId, String parameters, String operator) {
        command.execute();

        CommandLogEntity log = CommandLogEntity.builder()
                .commandName(command.getCommandName())
                .targetId(targetId)
                .parameters(parameters)
                .executedBy(normalizeOperator(operator))
                .undoable(command.isUndoable())
                .build();

        CommandLogEntity savedLog = commandLogRepository.save(log);
        if (command.isUndoable()) {
            commandSnapshotRepository.save(CommandSnapshotEntity.builder()
                    .commandLog(savedLog)
                    .mementoState(serialize(command.createMemento()))
                    .build());
        }
    }

    public boolean undoLastCommand(String operator, Function<CommandLogEntity, GridCommand> commandFactory) {
        String executingUser = normalizeOperator(operator);
        Optional<CommandSnapshotEntity> latest =
                commandSnapshotRepository.findFirstByCommandLog_ExecutedByAndConsumedAtIsNullOrderByCreatedAtDesc(
                        executingUser);
        if (latest.isEmpty()) {
            return false;
        }

        CommandSnapshotEntity snapshot = latest.get();
        CommandLogEntity original = snapshot.getCommandLog();
        GridCommand command = commandFactory.apply(original);
        command.undo(deserialize(snapshot.getMementoState()));
        snapshot.setConsumedAt(java.time.ZonedDateTime.now());
        commandSnapshotRepository.save(snapshot);
        original.setUndoable(false);
        commandLogRepository.save(original);
        commandLogRepository.save(CommandLogEntity.builder()
                .commandName("UNDO_" + original.getCommandName())
                .targetId(original.getTargetId())
                .parameters("Undo executed successfully")
                .executedBy(executingUser)
                .undoable(false)
                .build());
        return true;
    }

    private String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? "OPERATOR" : operator;
    }

    private String serialize(Map<String, Object> memento) {
        try {
            return objectMapper.writeValueAsString(memento);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo persistir el estado reversible del comando.", exception);
        }
    }

    private Map<String, Object> deserialize(String mementoState) {
        try {
            return objectMapper.readValue(mementoState, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo recuperar el estado reversible del comando.", exception);
        }
    }
}
