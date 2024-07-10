package app.coveredbridge.services;

import app.coveredbridge.data.models.Host;
import app.coveredbridge.data.models.Organization;
import app.coveredbridge.data.models.Proxy;
import app.coveredbridge.data.types.ConfigType;
import app.coveredbridge.data.types.HostType;
import app.coveredbridge.data.types.ProxyType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ConfigFileLoader {
  private static final Logger LOGGER = Logger.getLogger(ConfigFileLoader.class);

  @Inject
  SnowflakeIdGenerator snowflakeIdGenerator;

  @Inject
  ObjectMapper mapper;

  @ConfigProperty(name = "covered-bridge.config.file")
  String configFile;

  @WithSpan("ConfigFileLoader.onStart")
  public void onStart(@Observes StartupEvent event, Vertx vertx, Mutiny.SessionFactory factory) {
    // Create a new Vertx context for Hibernate Reactive
    Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
    // Mark the context as safe
    VertxContextSafetyToggle.setContextSafe(context, true);
    // Run the logic on the created context
    context.runOnContext(v -> handleStart(factory));
  }

  private void handleStart(Mutiny.SessionFactory factory) {
    // Start a new transaction
    factory.withTransaction(session -> loadConfig().onItem().transform(config -> {
        LOGGER.info("Config loaded: " + config);
        return config;
      }))
      // Subscribe to the Uni to trigger the action
      .subscribe().with(v -> {
      });
  }

  public Uni<ConfigType> parseConfigJson(File file) {
    return Uni.createFrom().item(() -> {
      try {
        return mapper.readValue(file, ConfigType.class);
      } catch (IOException e) {
        // Throw an unchecked exception to ensure the Uni fails
        throw new RuntimeException("Failed to parse JSON", e);
      }
    });
  }

  private Uni<Void> loadConfig() {
    return parseConfigJson(new File(configFile))
      .onItem().transformToUni(config -> {
        LOGGER.info("Config loaded: " + config);

        List<Uni<Void>> orgUnis = new ArrayList<>();

        config.getOrganizations().forEach(orgFromJson ->
          orgUnis.add(
            Organization.findOrCreateByKey(orgFromJson.getKey(), snowflakeIdGenerator)
              .onItem().transformToUni(org -> {

                List<Uni<Void>> proxyUniList = new ArrayList<>();
                for (ProxyType proxyFromJson : orgFromJson.getProxies()) {
                  proxyUniList.add(
                    Proxy.findOrCreateByKey(org, proxyFromJson.getKey(), snowflakeIdGenerator)
                      .onItem().transform(proxy -> upsertHosts(proxyFromJson, proxy))
                      .onItem().ignore().andContinueWithNull()
                  );
                }

                return Uni.combine().all().unis(proxyUniList).with(ignored -> org);
              }).onItem().ignore().andContinueWithNull()
          )
        );

        return Uni.combine().all().unis(orgUnis).discardItems();
      });
  }

  private Uni<Proxy> upsertHosts(ProxyType proxyFromJson, Proxy proxy) {
    List<Uni<Void>> hostUniList = new ArrayList<>();
    for (HostType hostFromJson : proxyFromJson.getHosts()) {
      hostUniList.add(
        Host.findOrCreateByName(proxy, hostFromJson.getName(), snowflakeIdGenerator)
          .onItem().ignore().andContinueWithNull()
      );
    }

    return Uni.combine().all().unis(hostUniList).with(ignored -> proxy);

  }


}
