package app.coveredbridge.web;

import jakarta.servlet.ServletContext;
import org.ocpsoft.rewrite.annotation.RewriteConfiguration;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.config.Direction;
import org.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider;
import org.ocpsoft.rewrite.servlet.config.Path;
import org.ocpsoft.rewrite.servlet.config.Redirect;

@RewriteConfiguration
public class RewriteConfigurationProvider extends HttpConfigurationProvider {

    @Override
    public int priority() {
      return 10;
    }

    @Override
    public Configuration getConfiguration(ServletContext context) {
      return ConfigurationBuilder.begin()
        .addRule()
        .when(Direction.isInbound().and(Path.matches("/app")))
        .perform(Redirect.temporary(context.getContextPath() + "/app/index.xhtml"));
    }
}
