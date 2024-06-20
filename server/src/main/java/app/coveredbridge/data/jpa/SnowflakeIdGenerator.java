package app.coveredbridge.data.jpa;

import app.coveredbridge.constants.ApplicationConstants;
import app.coveredbridge.services.ServerInstance;
import com.callicoder.snowflake.Snowflake;
import org.hibernate.HibernateException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Hashtable;

@ApplicationScoped
public class SnowflakeIdGenerator {

  @Inject
  ServerInstance serverInstance;

  Hashtable<String, Snowflake> snowflakes = new Hashtable<>();

  public Long generate(String idName) throws HibernateException {
    Snowflake snowflake;
    if (snowflakes.containsKey(idName)) {
      snowflake = snowflakes.get(idName);
    } else {
      snowflake = new Snowflake(serverInstance.getServer().instanceNumber, ApplicationConstants.APP_EPOCH);
      snowflakes.put(idName, snowflake);
    }
    return snowflake.nextId();
  }
}
