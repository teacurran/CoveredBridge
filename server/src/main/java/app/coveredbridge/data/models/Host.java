package app.coveredbridge.data.models;

import app.coveredbridge.services.SnowflakeIdGenerator;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "hosts")
public class Host extends DefaultPanacheEntityWithTimestamps {

  @ManyToOne
  public Proxy proxy;

  public String name;

  @Column(name = "is_validated",
    nullable = false, columnDefinition="BOOLEAN DEFAULT false")
  public boolean isValidated;

  @ManyToOne
  public Account account;

  public static Uni<Host> findByValidatedName(String value) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("name", value);
    parameters.put("validated", true);
    return find("name=:name AND isValidated=:validated", parameters).firstResult();
  }

  public static Uni<Host> findByName(String value) {
    return find("name", value).firstResult();
  }

  public static Uni<Host> findOrCreateByName(Proxy proxy, String name, SnowflakeIdGenerator idGenerator) {
    return findByName(name)
      .onItem().ifNotNull().transform(host -> host)
      .onItem().ifNull().switchTo(() -> {
        Host newItem = new Host();
        newItem.id = idGenerator.generate(Host.class.getSimpleName());
        newItem.proxy = proxy;
        newItem.name = name;
        // Set other default values if necessary
        return newItem.persist();
      });
  }

}
