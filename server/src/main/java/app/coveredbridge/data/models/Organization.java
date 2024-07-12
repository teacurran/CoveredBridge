package app.coveredbridge.data.models;

import app.coveredbridge.services.SnowflakeIdGenerator;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organizations")
public class Organization extends DefaultPanacheEntityWithTimestamps {

  @Column(nullable = true, unique = true)
  public String key;

  public String name;

  @OneToMany(mappedBy = "organization")
  public List<Proxy> proxies = new ArrayList<>();

  public static Uni<Organization> findByKey(String key) {
    return find("key", key).firstResult();
  }

  public static Uni<Organization> findOrCreateByKey(String key, SnowflakeIdGenerator idGenerator) {
    return findByKey(key)
      .onItem().ifNotNull().transform(organization -> organization)
      .onItem().ifNull().switchTo(() -> {
        Organization newOrg = new Organization();
        newOrg.id = idGenerator.generate(Organization.class.getSimpleName());
        newOrg.key = key;
        // Set other default values if necessary
        return newOrg.persist();
      });
  }
}
