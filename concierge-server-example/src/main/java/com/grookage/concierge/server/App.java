package com.grookage.concierge.server;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.grookage.conceirge.dwserver.permissions.PermissionValidator;
import com.grookage.conceirge.dwserver.resolvers.ConfigUpdaterResolver;
import com.grookage.concierge.aerospike.client.AerospikeConfig;
import com.grookage.concierge.aerospikedw.ConciergeAerospikeBundle;
import com.grookage.concierge.core.cache.CacheConfig;
import com.grookage.concierge.core.engine.validator.ConfigDataValidator;
import com.grookage.concierge.models.config.ConfigKey;
import com.grookage.concierge.models.ingestion.ConfigurationRequest;
import com.grookage.concierge.models.ingestion.UpdateConfigRequest;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.HttpHeaders;
import java.util.function.Supplier;

@Slf4j
public class App extends Application<AppConfiguration> {

    public static void main(final String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void initialize(final Bootstrap<AppConfiguration> bootstrap) {
        final var bundle = new ConciergeAerospikeBundle<AppConfiguration, ConciergeConfigUpdater>() {
            @Override
            protected Supplier<ConfigUpdaterResolver<ConciergeConfigUpdater>> userResolver(AppConfiguration configuration) {
                return () -> httpHeaders -> new ConciergeConfigUpdater();
            }

            @Override
            protected CacheConfig getCacheConfig(AppConfiguration configuration) {
                return CacheConfig.builder()
                        .enabled(true)
                        .refreshCacheSeconds(10)
                        .build();
            }

            @Override
            protected Supplier<PermissionValidator<ConciergeConfigUpdater>> getPermissionResolver(AppConfiguration configuration) {
                return () -> new PermissionValidator<>() {
                    @Override
                    public void authorize(HttpHeaders headers, ConciergeConfigUpdater schemaUpdater, ConfigurationRequest configurationRequest) {
                        //NOOP
                    }

                    @Override
                    public void authorize(HttpHeaders headers, ConciergeConfigUpdater schemaUpdater, UpdateConfigRequest configRequest) {
                        //NOOP
                    }

                    @Override
                    public void authorizeApproval(HttpHeaders headers, ConciergeConfigUpdater schemaUpdater, ConfigKey schemaKey) {
                        //NOOP
                    }

                    @Override
                    public void authorizeRejection(HttpHeaders headers, ConciergeConfigUpdater schemaUpdater, ConfigKey schemaKey) {
                        //NOOP
                    }

                    @Override
                    public void authorizeActivation(HttpHeaders headers, ConciergeConfigUpdater schemaUpdater, ConfigKey schemaKey) {
                        //NOOP
                    }
                };
            }

            @Override
            protected Supplier<ConfigDataValidator> getConfigDataValidator(AppConfiguration configuration) {
                return () -> (configKey, configData) -> {
                };
            }

            @Override
            protected AerospikeConfig getAerospikeConfig(AppConfiguration configuration) {
                return configuration.getAerospikeConfig();
            }
        };
        bootstrap.addBundle(bundle);
    }

    @Override
    public void run(AppConfiguration appConfiguration, Environment environment) {
        environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        environment.getObjectMapper().registerModule(new GuavaModule())
                .registerModule(new Jdk8Module())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
    }

}
