package app.coveredbridge.services;

import app.coveredbridge.data.models.ProxyHost;
import app.coveredbridge.data.models.Organization;
import app.coveredbridge.data.models.Proxy;
import app.coveredbridge.data.models.ProxyPath;
import app.coveredbridge.data.types.ConfigType;
import app.coveredbridge.data.types.HostType;
import app.coveredbridge.data.types.ProxyPathType;
import app.coveredbridge.data.types.ProxyType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
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

import static io.smallrye.mutiny.helpers.spies.Spy.onItem;

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
                List<Uni<Proxy>> proxyUniList = new ArrayList<>();
                for (ProxyType proxyFromJson : orgFromJson.getProxies()) {
                  proxyUniList.add(findOrCreateProxy(org, proxyFromJson));
                }

                return Uni.combine().all().unis(proxyUniList).with(ignored -> null);
              })
          )
        );

        return Uni.combine().all().unis(orgUnis).discardItems();
      });
  }

  private Uni<Proxy> findOrCreateProxy(Organization org, ProxyType proxyFromJson) {
    return Proxy.findOrCreateByKey(org, proxyFromJson.getKey(), snowflakeIdGenerator)
      .onItem().transformToUni(proxy -> {
        return findOrCreateProxyPaths(proxyFromJson, proxy)
          .chain(() -> upsertHosts(proxyFromJson, proxy))
          .chain(() -> Uni.createFrom().item(proxy));
      });
  }

  private Uni<Void> findOrCreateProxyPaths(ProxyType proxyFromJson, Proxy proxy) {
    List<Uni<ProxyPath>> pathUniList = new ArrayList<>();
    for (ProxyPathType pathFromJson : proxyFromJson.getPaths()) {
      pathUniList.add(
        ProxyPath.findOrCreateByProxyAndPath(proxy, pathFromJson.getPath(), snowflakeIdGenerator)
          .onItem().transformToUni(path -> path.updateFromJson(pathFromJson))
          .onFailure().recoverWithUni(throwable -> {
            LOGGER.error("Failed to create path: " + pathFromJson.getPath(), throwable);
            return Uni.createFrom().failure(throwable);
          })
      );
    }

    return Multi.createFrom().iterable(pathUniList).onItem().transformToUniAndConcatenate(proxyPathUni -> {
      return proxyPathUni;
    }).collect().asList().replaceWith(Uni.createFrom().voidItem());
  }

  private Uni<Void> upsertHosts(ProxyType proxyFromJson, Proxy proxy) {
    List<Uni<ProxyHost>> hostUniList = new ArrayList<>();
    for (HostType hostFromJson : proxyFromJson.getHosts()) {
      hostUniList.add(
        ProxyHost.findOrCreateByName(proxy, hostFromJson.getName(), snowflakeIdGenerator)
          .onItem().transformToUni(host -> host.updateFromJson(hostFromJson))
          .onFailure().recoverWithUni(throwable -> {
            LOGGER.error("Failed to create host: " + hostFromJson.getName(), throwable);
            return Uni.createFrom().failure(throwable);
          })
      );
    }

    return Multi.createFrom().iterable(hostUniList).onItem().transformToUniAndConcatenate(proxyHostUni -> {
      return Uni.createFrom().voidItem();
    }).toUni();

//    merged.toUni()
//
//    //return Uni.combine().all().unis(hostUniList).collectFailures().discardItems();
  }


}
