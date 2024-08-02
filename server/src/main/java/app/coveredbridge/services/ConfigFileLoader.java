package app.coveredbridge.services;

import app.coveredbridge.data.models.Account;
import app.coveredbridge.data.models.Group;
import app.coveredbridge.data.models.Organization;
import app.coveredbridge.data.models.Site;
import app.coveredbridge.data.models.SiteHost;
import app.coveredbridge.data.models.SitePath;
import app.coveredbridge.data.types.ConfigType;
import app.coveredbridge.data.types.HostType;
import app.coveredbridge.data.types.OrganizationType;
import app.coveredbridge.data.types.SitePathType;
import app.coveredbridge.data.types.SiteType;
import app.coveredbridge.utils.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
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

  @Inject
  Tracer tracer;

  @Inject
  EncryptionUtil encryptionUtil;

  @WithSpan("ConfigFileLoader.onStart")
  public void onStart(@Observes StartupEvent event, Vertx vertx, Mutiny.SessionFactory factory) {
    // Create a new Vertx context for Hibernate Reactive
    io.vertx.core.Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
    // Mark the context as safe
    VertxContextSafetyToggle.setContextSafe(context, true);
    // Run the logic on the created context
    context.runOnContext(v -> handleStart(factory));
  }

  @WithSpan
  public void handleStart(Mutiny.SessionFactory factory) {
    // Start a new transaction
    factory.withTransaction(session -> loadConfig().onItem().transform(config -> {
        LOGGER.info("Config loaded: " + config);
        return config;
      }))
      // Subscribe to the Uni to trigger the action
      .subscribe().with(v -> {
      });
  }

  @WithSpan
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

  @WithSpan
  public Uni<Void> loadConfig() {
    return parseConfigJson(new File(configFile))
      .onItem().transformToUni(config -> {
        LOGGER.info("Config loaded: " + config);

        return Multi.createFrom().iterable(config.getOrganizations())
          .onItem().transformToUniAndConcatenate(orgFromJson ->
            Organization.findOrCreateByKey(orgFromJson.getKey(), snowflakeIdGenerator)
              .invoke(org -> LOGGER.info("Organization created: " + org))
              .chain(org -> findOrCreateGroups(org, orgFromJson))
              .invoke(org -> LOGGER.info("Groups created for organization: " + org))
              .chain(org -> findOrCreateAccounts(org, orgFromJson))
              .chain(org -> {
                List<Uni<Site>> proxyUniList = new ArrayList<>();
                for (SiteType proxyFromJson : orgFromJson.getSites()) {
                  proxyUniList.add(findOrCreateProxy(org, proxyFromJson));
                }

                return Uni.combine().all().unis(proxyUniList).with(ignored -> null);
              }).replaceWith(Uni.createFrom().nullItem())
          ).collect().asList().replaceWith(Uni.createFrom().nullItem());
      });
  }

  @WithSpan
  public Uni<Site> findOrCreateProxy(Organization org, SiteType proxyFromJson) {
    return Site.findOrCreateByKey(org, proxyFromJson.getKey(), snowflakeIdGenerator)
      .onItem()
      .call(proxy -> findOrCreateProxyHosts(proxy, proxyFromJson))
      .call(proxy -> findOrCreateProxyPaths(proxy, proxyFromJson).replaceWith(Uni.createFrom().item(proxy)));
  }

  @WithSpan
  public Uni<Organization> findOrCreateAccounts(Organization org, OrganizationType orgFromJson) {
    return Multi.createFrom().iterable(orgFromJson.getAccounts())
      .onItem()
      .transformToUniAndConcatenate(accountFromJson -> {
        Span span = tracer.spanBuilder("findOrCreateAccounts")
          .setParent(Context.current().with(Span.current()))
          .setSpanKind(SpanKind.INTERNAL)
          .startSpan();
        Uni<Account> result = Account.createOrUpdateFromJson(org, accountFromJson, snowflakeIdGenerator)
            .chain(account -> account.persistAndFlushWithEncryption(encryptionUtil));
        span.end();
        return result;
      })
      .collect().asList().replaceWith(Uni.createFrom().item(org));
  }

  @WithSpan
  public Uni<Organization> findOrCreateGroups(Organization org, OrganizationType orgFromJson) {
    return Multi.createFrom().iterable(orgFromJson.getGroups())
      .onItem()
      .transformToUniAndConcatenate(groupFromJson -> {
        return Group.findOrCreateByName(org, groupFromJson.getName(), snowflakeIdGenerator);
      })
      .collect().asList().replaceWith(Uni.createFrom().item(org));
  }

  @WithSpan
  public Uni<Site> findOrCreateProxyPaths(Site site, SiteType proxyFromJson) {
    List<Uni<SitePath>> pathUniList = new ArrayList<>();
    for (SitePathType pathFromJson : proxyFromJson.getPaths()) {
      pathUniList.add(
        SitePath.findOrCreateBySiteAndPath(site, pathFromJson.getPath(), snowflakeIdGenerator)
          .onItem().transformToUni(path -> path.updateFromJson(pathFromJson))
          .onFailure().recoverWithUni(throwable -> {
            LOGGER.error("Failed to create path: " + pathFromJson.getPath(), throwable);
            return Uni.createFrom().failure(throwable);
          })
      );
    }

    return Multi.createFrom().iterable(pathUniList)
      .onItem()
      .transformToUniAndConcatenate(proxyPathUni -> proxyPathUni)
      .collect().asList().replaceWith(Uni.createFrom().item(site));
  }

  @WithSpan
  public Uni<Site> findOrCreateProxyHosts(Site site, SiteType proxyFromJson) {
    List<Uni<SiteHost>> hostUniList = new ArrayList<>();
    for (HostType hostFromJson : proxyFromJson.getHosts()) {
      hostUniList.add(
        SiteHost.findOrCreateByName(site, hostFromJson.getName(), snowflakeIdGenerator)
          .onItem().transformToUni(host -> host.updateFromJson(hostFromJson))
          .onFailure().recoverWithUni(throwable -> {
            LOGGER.error("Failed to create host: " + hostFromJson.getName(), throwable);
            return Uni.createFrom().failure(throwable);
          })
      );
    }

    return Multi.createFrom().iterable(hostUniList)
      .onItem()
      .transformToUniAndConcatenate(proxyHostUni -> proxyHostUni)
      .collect().asList().replaceWith(Uni.createFrom().item(site));
  }


}
