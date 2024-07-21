package app.coveredbridge.services.web;

import app.coveredbridge.data.models.Proxy;
import app.coveredbridge.data.models.ProxyPath;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/")
public class GatedProxy {
  private static final Logger LOGGER = Logger.getLogger(GatedProxy.class);

  @Inject
  Vertx vertx;

  private WebClient client;

  @PostConstruct
  public void init() {
    WebClientOptions options = new WebClientOptions().setFollowRedirects(true);
    client = WebClient.create(vertx, options);
  }

  @GET
  @Path("/{path:.*}")
  public Uni<Response> proxy(@Context UriInfo uriInfo, @PathParam("path") String path) {
    String hostname = uriInfo.getBaseUri().getHost();

    LOGGER.info("Proxying request for host:%s path:%s".formatted(hostname, path));
    return Panache.withTransaction(() -> Proxy.findByHostByName(hostname)
      .onItem().transformToUni(proxy -> {
        if (proxy == null) {
          return Uni.createFrom().item(Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Host not found: " + hostname).build());
        }
        return findMatchingPath(proxy, path)
          .onItem().transformToUni(proxyPath -> {
            if (proxyPath == null) {
              return Uni.createFrom().item(Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Path not found: " + path).build());
            }

            return fetchContentFromPath(path);
          })
          .onItem().transformToUni(content -> Uni.createFrom().item(Response.ok(content).build()));
      }).onFailure().invoke(throwable -> LOGGER.error("Error occurred", throwable))
      .onFailure().recoverWithUni(() -> Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND)
        .entity("Host not foundX: " + hostname).build()))
    );
  }

  public Uni<ProxyPath> findMatchingPath(Proxy proxy, String path) {
    return Uni.createFrom().item(proxy.paths.stream()
      .filter(p -> {
        return path.startsWith(p.path);
      })
      .findFirst()
      .orElse(null));
  }

  @WithSpan("GatedProxy.fetchContentFromPath")
  public Uni<String> fetchContentFromPath(String path) {
    return client.getAbs("https://quarkus.io/" + path)
      .send()
      .onItem().transformToUni(res -> {
        if (res.statusCode() == 301 || res.statusCode() == 302) {
          String location = res.getHeader("Location");
          String newPath = location.replace("https://quarkus.io/", "");
          return fetchContentFromPath(newPath);
        }
        return Uni.createFrom().item(modifyContent(res.bodyAsString()));
      });
  }

  @WithSpan("GatedProxy.modifyContent")
  public String modifyContent(String content) {
    Pattern pattern = Pattern.compile("\\b\\w{6}\\b(?![^<]*>)");
    Matcher matcher = pattern.matcher(content);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(sb, matcher.group() + "™");
    }
    matcher.appendTail(sb);

    String modifiedContent = sb.toString();

    modifiedContent = modifiedContent.replaceAll("href=\"/(?!proxy/)", "href=\"/proxy/");
    modifiedContent = modifiedContent.replaceAll("src=\"/(?!proxy/)", "src=\"/proxy/");

    modifiedContent = modifiedContent.replaceAll("href=\"https://quarkus.io/", "href=\"/proxy/");
    modifiedContent = modifiedContent.replaceAll("src=\"https://quarkus.io/", "src=\"/proxy/");

    modifiedContent = modifiedContent.replaceAll("<meta http-equiv=\"Content-Security-Policy\"[^>]+>", "");

    return modifiedContent;
  }

  private Uni<Response> fetchImageFromPath(String path) {
    return client.getAbs("https://quarkus.io/" + path)
      .send()
      .onItem().transformToUni(res -> {
        byte[] imageData = res.body().getBytes();
        String contentType = res.getHeader("Content-Type");
        return Uni.createFrom().item(Response.ok(imageData).type(contentType).build());
      });
  }
}
