package app.coveredbridge.services;


import app.coveredbridge.data.models.Server;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import org.hibernate.reactive.mutiny.Mutiny;

import java.time.Duration;
import java.time.LocalDateTime;

@ApplicationScoped
public class ServerInstance {

  Duration DEFAULT_DB_WAIT = Duration.ofSeconds(10);

  Server server;

  @Produces
  public Server getServer() {
    return server;
  }

  @WithSpan("ServerInstance.onStart")
  public void onStart(@Observes StartupEvent event, Vertx vertx, Mutiny.SessionFactory factory) {
    // We need a duplicated vertx context for hibernate reactive
    Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
    // Don't forget to mark the context safe
    VertxContextSafetyToggle.setContextSafe(context, true);
    // Run the logic on the context created above
    context.runOnContext(new Handler<Void>() {
      @Override
      public void handle(Void event) {
        // We cannot use the Panache.withTransaction() and friends because the CDI request context is not active/propagated
        factory.withTransaction(session ->
            Server.findMaxInstanceId().call(maxIntDto -> {
              if (maxIntDto == null) {
                server = new Server();
                server.instanceNumber = 1;
                server.seen = LocalDateTime.now();
                server.isShutdown = false;
                return server.persistAndFlush();
              } else {
                if (maxIntDto.maxValue < 1000) {
                  server = new Server();
                  server.instanceNumber = maxIntDto.maxValue + 1;
                  server.seen = LocalDateTime.now();
                  server.isShutdown = false;
                  return server.persistAndFlush();
                } else {
                  return Server.findFirstUnusedServer().call(s -> {
                    if (s == null) {
                      throw new RuntimeException("No server instance available");
                    }
                    server = s;
                    server.seen = LocalDateTime.now();
                    server.isShutdown = false;
                    return server.persistAndFlush();
                  });
                }
              }
            })
          )
          // We need to subscribe to the Uni to trigger the action
          .subscribe().with(v -> {});
      }
    });
  }

  @WithSpan("ServerInstance.onStop")
  public void onStop(@Observes ShutdownEvent event, Vertx vertx, Mutiny.SessionFactory factory) {
    if (server != null) {
      // We need a duplicated vertx context for hibernate reactive
      Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
      // Don't forget to mark the context safe
      VertxContextSafetyToggle.setContextSafe(context, true);
      // Run the logic on the context created above
      context.runOnContext(new Handler<Void>() {
        @Override
        public void handle(Void event) {
          // We cannot use the Panache.withTransaction() and friends because the CDI request context is not active/propagated
          factory.withTransaction(session ->
            Server.byId(server.id).call(s -> {
              s.isShutdown = true;
              return s.persistAndFlush();
            })
          ).subscribe().with(v -> {});
        }
      });
    }
  }
}
