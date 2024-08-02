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
@Table(name = "site_hosts")
public class SiteHost extends DefaultPanacheEntityWithTimestamps {

  @ManyToOne
  public Site site;

  public String name;

  @Column(name = "is_validated",
    nullable = false, columnDefinition="BOOLEAN DEFAULT false")
  public boolean isValidated;

  public static Uni<SiteHost> findByValidatedName(String value) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("name", value);
    parameters.put("validated", true);
    return find("name=:name AND isValidated=:validated", parameters).firstResult();
  }

  public static Uni<SiteHost> findByName(String value) {
    return find("name", value).firstResult();
  }

  public static Uni<SiteHost> findOrCreateByName(Site site, String name, SnowflakeIdGenerator idGenerator) {
    return findByName(name)
      .onItem().ifNotNull().transform(host -> host)
      .onItem().ifNull().switchTo(() -> {
        SiteHost newItem = new SiteHost();
        newItem.id = idGenerator.generate(SiteHost.class.getSimpleName());
        newItem.site = site;
        newItem.name = name;
        // Set other default values if necessary
        return newItem.persist();
      });
  }

  public Uni<SiteHost> updateFromJson(HostType hostFromJson) {
    this.name = hostFromJson.getName();
    this.isValidated = hostFromJson.getIsValidated();
    return this.persistAndFlush();
  }
}
