package app.coveredbridge.services.web;

import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;

public class VertxReponseTransformer {
  public static Response transform(HttpResponse<Buffer> vertxResponse) {
    int statusCode = vertxResponse.statusCode();

    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    vertxResponse.headers().forEach(header -> headers.add(header.getKey(), header.getValue()));

    String contentType = vertxResponse.getHeader("Content-Type");
    Response.ResponseBuilder responseBuilder = Response.status(statusCode);

    if (contentType != null && (contentType.startsWith("image/") || contentType.equals("application/octet-stream"))) {
      byte[] body = vertxResponse.body().getBytes();
      responseBuilder.entity(body).type(contentType);
    } else {
      String body = vertxResponse.bodyAsString();
      responseBuilder.entity(body);
    }

    return responseBuilder.build();
  }

  public static RestResponse<Buffer> transformToRestResponse(HttpResponse<Buffer> vertxResponse) {
    int statusCode = vertxResponse.statusCode();
    Response.StatusType statusType = Response.Status.fromStatusCode(statusCode);

    RestResponse.ResponseBuilder<Buffer> responseBuilder = RestResponse.ResponseBuilder.create(statusType);
    responseBuilder.entity(vertxResponse.body());
    vertxResponse.headers().forEach(header -> responseBuilder.header(header.getKey(), header.getValue()));

   return responseBuilder.build();
  }

}
