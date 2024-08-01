package app.coveredbridge.data.models;

import app.coveredbridge.data.types.HostType;
import app.coveredbridge.services.SnowflakeIdGenerator;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "proxy_hosts")
public class ProxyHost extends DefaultPanacheEntityWithTimestamps {

  @ManyToOne
  public Site site;

  public String name;

  @Column(name = "is_validated",
    nullable = false, columnDefinition="BOOLEAN DEFAULT false")
  public boolean isValidated;

  public static Uni<ProxyHost> findByValidatedName(String value) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("name", value);
    parameters.put("validated", true);
    return find("name=:name AND isValidated=:validated", parameters).firstResult();
  }

  public static Uni<ProxyHost> findByName(String value) {
    return find("name", value).firstResult();
  }

  public static Uni<ProxyHost> findOrCreateByName(Site site, String name, SnowflakeIdGenerator idGenerator) {
    return findByName(name)
      .onItem().ifNotNull().transform(host -> host)
      .onItem().ifNull().switchTo(() -> {
        ProxyHost newItem = new ProxyHost();
        newItem.id = idGenerator.generate(ProxyHost.class.getSimpleName());
        newItem.site = site;
        newItem.name = name;
        // Set other default values if necessary
        return newItem.persist();
      });
  }

  public Uni<ProxyHost> updateFromJson(HostType hostFromJson) {
    this.name = hostFromJson.getName();
    this.isValidated = hostFromJson.getIsValidated();
    return this.persistAndFlush();
  }
}
