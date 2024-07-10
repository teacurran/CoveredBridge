package app.coveredbridge.services;

import app.coveredbridge.data.models.Server;
import app.coveredbridge.data.models.dto.MaxIntDto;
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
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.LocalDateTime;

@ApplicationScoped
public class ServerInstance {
  private static final Logger LOGGER = Logger.getLogger(ServerInstance.class);

  Duration DEFAULT_DB_WAIT = Duration.ofSeconds(10);

  Server server;

  @Produces
  @ApplicationScoped
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
        })
      )
      // Subscribe to the Uni to trigger the action
      .subscribe().with(v -> {});
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
      .subscribe().with(v -> {
      });
  }
}
