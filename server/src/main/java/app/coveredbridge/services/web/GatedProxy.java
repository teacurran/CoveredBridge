package app.coveredbridge.services.web;

import app.coveredbridge.data.models.Site;
import app.coveredbridge.data.models.SitePath;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

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
  public Uni<RestResponse<Buffer>> proxyGet(@Context UriInfo uriInfo, @PathParam("path") String path) {
    String hostname = uriInfo.getBaseUri().getHost();

    LOGGER.info("Proxying request for host:%s path:%s".formatted(hostname, path));
    return Panache.withTransaction(() -> Site.findByHostByName(hostname)
      .onItem().transformToUni(proxy -> {
        if (proxy == null) {
          Buffer buffer = Buffer.buffer("Host not found: " + hostname);

          return Uni.createFrom().item(RestResponse.status(Response.Status.SERVICE_UNAVAILABLE, buffer));
        }
        return findMatchingPath(proxy, path)
          .onItem().transformToUni(proxyPath -> {
            if (proxyPath == null) {
              Buffer buffer = Buffer.buffer("Path not found: " + path);

              return Uni.createFrom().item(RestResponse.status(Response.Status.SERVICE_UNAVAILABLE, buffer));
            }

            Uni<RestResponse<Buffer>> content = fetchContent(proxyPath.target, path);
            return content;
          });
      }).onFailure().invoke(throwable -> LOGGER.error("Error occurred", throwable))
      .onFailure().recoverWithUni(error -> {
        Buffer buffer = Buffer.buffer("An Error occured: " + path);

        return Uni.createFrom().item(RestResponse.ResponseBuilder.ok(buffer).build());
      })
    );
  }

  @POST
  @Path("/{path:.*}")
  public Uni<RestResponse<Buffer>> proxyPost(@Context UriInfo uriInfo, @PathParam("path") String path) {
    return proxyGet(uriInfo, path);
  }


  public Uni<SitePath> findMatchingPath(Site site, String path) {
    return Uni.createFrom().item(site.paths.stream()
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

  private Uni<RestResponse<Buffer>> fetchContent(String baseUrl, String path) {
    String url = baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
    return client.getAbs(url)
      .send()
      .onItem()
      .transform(res -> VertxReponseTransformer.transformToRestResponse(res));
  }
}
