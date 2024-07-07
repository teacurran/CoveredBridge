package app.coveredbridge.services;

import app.coveredbridge.data.models.Server;
import app.coveredbridge.data.models.dto.MaxIntDto;
import app.coveredbridge.data.types.ConfigType;
import app.coveredbridge.services.web.GatedProxy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Scanner;

@ApplicationScoped
public class ServerInstance {
  private static final Logger LOGGER = Logger.getLogger(ServerInstance.class);

  Duration DEFAULT_DB_WAIT = Duration.ofSeconds(10);

  Server server;

  @ConfigProperty(name = "covered-bridge.config.file")
  String configFile;

  @Inject
  ObjectMapper mapper;

  @Produces
  public Server getServer() {
    return server;
  }

  @WithSpan("ServerInstance.onStart")
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
    factory.withTransaction(session ->
        // Find the maximum instance ID
        Server.findMaxInstanceId().call(maxIntDto -> {
          if (maxIntDto == null || maxIntDto.maxValue < 1000) {
            // If there is no max instance ID or it's less than 1000, create a new server
            return createNewServer(maxIntDto);
          } else {
            // Otherwise, find the first unused server
            return findFirstUnusedServer();
          }
        }).onItem().transformToUni(s -> {
          return loadConfig().onItem().transform(config -> {
            LOGGER.info("Config loaded: " + config);
            return s;
          });
        })
      )
      // Subscribe to the Uni to trigger the action
      .subscribe().with(v -> {});
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

  private Uni<Object> loadConfig() {
    return Uni.createFrom().item(() -> parseConfigJson(new File(configFile)))
      .onItem().transformToUni(config -> {
        LOGGER.info("Config loaded: " + config);

        return Uni.createFrom().nullItem();
      }).onFailure().invoke(throwable -> LOGGER.error("Error loading config", throwable));
  }

  private Uni<Server> createNewServer(MaxIntDto maxIntDto) {
    server = new Server();
    server.instanceNumber = maxIntDto == null ? 1 : maxIntDto.maxValue + 1;
    server.seen = LocalDateTime.now();
    server.isShutdown = false;
    return server.persistAndFlush();
  }

  private Uni<Server> findFirstUnusedServer() {
    return Server.findFirstUnusedServer().call(s -> {
      if (s == null) {
        return Uni.createFrom().failure(new RuntimeException("No server instance available"));
      }
      server = s;
      server.seen = LocalDateTime.now();
      server.isShutdown = false;
      return server.persistAndFlush();
    });
  }

  @WithSpan("ServerInstance.onStop")
  public void onStop(@Observes ShutdownEvent event, Vertx vertx, Mutiny.SessionFactory factory) {
    if (server != null) {
      // Create a new Vertx context for Hibernate Reactive
      Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
      // Mark the context as safe
      VertxContextSafetyToggle.setContextSafe(context, true);
      // Run the logic on the created context
      context.runOnContext(v -> handleStop(factory));
    }
  }

  private void handleStop(Mutiny.SessionFactory factory) {
    // Start a new transaction
    factory.withTransaction(session ->
        // Find the server by ID
        Server.byId(server.id).call(s -> {
          s.isShutdown = true;
          return s.persistAndFlush();
        })
      )
      // Subscribe to the Uni to trigger the action
      .subscribe().with(v -> {});
  }
}
