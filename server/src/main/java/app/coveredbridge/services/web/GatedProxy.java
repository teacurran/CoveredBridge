package app.coveredbridge.services.web;

import app.coveredbridge.data.models.Host;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Path("/")
public class GatedProxy {
  private static final Logger LOGGER = Logger.getLogger(GatedProxy.class);

  @Inject
  Vertx vertx;

  @GET
  @Path("{path:.*}")
  public Uni<Response> proxy(@Context UriInfo uriInfo, @PathParam("path") String path) {
    String hostname = uriInfo.getBaseUri().getHost();

    LOGGER.info("UriInfo: " + uriInfo.getRequestUri());

    return Panache.withTransaction(() -> Host.findByValidatedName(hostname)
      .onItem().ifNotNull().transformToUni(host -> {
        LOGGER.info("Host: " + host);

        if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".gif") || path.endsWith(".svg")) {
          return Uni.createFrom().completionStage(fetchImageFromPath(path));
        }

        return Uni.createFrom().completionStage(
          fetchContentFromPath(path).thenApply(Response::ok).thenApply(Response.ResponseBuilder::build)
        );
      }).onItem().ifNull().continueWith(() -> {
        LOGGER.info("Host not found: " + hostname);
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Host not found: " + hostname).build();
      }).onFailure().invoke(throwable -> LOGGER.error("Error occurred: ", throwable))
      .onFailure().recoverWithItem(() -> Response.status(Response.Status.NOT_FOUND)
        .entity("Host not found: " + hostname).build())
    );
  }

  private CompletionStage<String> fetchContentFromPath(String path) {
    WebClientOptions options = new WebClientOptions().setFollowRedirects(false);
    WebClient client = WebClient.create(vertx, options);

    return client.getAbs("https://quarkus.io/" + path)
      .send()
      .toCompletionStage()
      .thenCompose(response -> {
        if (response.statusCode() == 301 || response.statusCode() == 302) {
          String location = response.getHeader("Location");
          String newPath = location.replace("https://quarkus.io/", "");
          return fetchContentFromPath(newPath);
        }
        return CompletableFuture.completedFuture(modifyContent(response.bodyAsString()));
      });
  }

  private String modifyContent(String content) {
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

  private CompletionStage<Response> fetchImageFromPath(String path) {
    WebClientOptions options = new WebClientOptions().setFollowRedirects(true);
    WebClient client = WebClient.create(vertx, options);

    return client.getAbs("https://quarkus.io/" + path)
      .send()
      .toCompletionStage()
      .thenApply(response -> {
        byte[] imageData = response.body().getBytes();
        String contentType = response.getHeader("Content-Type");
        return Response.ok(imageData).type(contentType).build();
      });
  }
}
