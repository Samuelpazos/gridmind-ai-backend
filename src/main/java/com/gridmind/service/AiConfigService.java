package com.gridmind.service;

import com.gridmind.config.WorkspaceDataSourceConfig;
import com.gridmind.strategy.EvaluationStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AiConfigService {

    private record Settings(String strategy, boolean degraded) {}

    private static final Settings DEFAULT = new Settings("SUPERVISED_MODEL_STRATEGY", false);
    private static final String SYSTEM = "";

    private final ApplicationContext applicationContext;
    private final ConcurrentMap<String, Settings> settings = new ConcurrentHashMap<>();

    public AiConfigService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public EvaluationStrategy getActiveStrategy() {
        return strategyFor(currentAccountId());
    }

    public EvaluationStrategy strategyFor(String ownerAccountId) {
        Settings config = settings.getOrDefault(ownerAccountId, DEFAULT);
        String strategy = config.degraded() ? "THRESHOLD_STRATEGY" : config.strategy();
        try {
            return applicationContext.getBean(strategy, EvaluationStrategy.class);
        } catch (org.springframework.beans.BeansException exception) {
            return applicationContext.getBean("THRESHOLD_STRATEGY", EvaluationStrategy.class);
        }
    }

    public String getConfiguredStrategyName() {
        return settings.getOrDefault(currentAccountId(), DEFAULT).strategy();
    }

    public String getActiveStrategyName() {
        Settings config = settings.getOrDefault(currentAccountId(), DEFAULT);
        return config.degraded() ? "THRESHOLD_STRATEGY (DEGRADED_MODE)" : config.strategy();
    }

    public void setActiveStrategyName(String strategyName) {
        if (strategyName != null) {
            settings.compute(currentAccountId(), (account, current) ->
                    new Settings(strategyName, current != null && current.degraded()));
        }
    }

    public boolean isForceDegradedMode() {
        return settings.getOrDefault(currentAccountId(), DEFAULT).degraded();
    }

    public void setForceDegradedMode(boolean enabled) {
        settings.compute(currentAccountId(), (account, current) ->
                new Settings(current == null ? DEFAULT.strategy() : current.strategy(), enabled));
    }

    private static String currentAccountId() {
        var account = WorkspaceDataSourceConfig.currentAccount();
        return account == null ? SYSTEM : account.id();
    }
}
