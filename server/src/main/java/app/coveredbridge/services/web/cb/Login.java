package app.coveredbridge.services.web.cb;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.Path;

public class Login extends Controller {

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance login();
  }

  @Path("/cb/login")
  public TemplateInstance login() {
    return Templates.login();
  }

}
