package app.coveredbridge.data.models;

import app.coveredbridge.services.SnowflakeIdGenerator;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "key"}))
public class Proxy extends DefaultPanacheEntityWithTimestamps{

  @ManyToOne
  public Organization organization;

  public String key;

  @OneToMany(
    mappedBy = "proxy",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  public List<Host> hosts = new ArrayList<>();

  @OneToMany(
    mappedBy = "proxy",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  public List<Path> paths = new ArrayList<>();

  public static Uni<Proxy> findByKey(String key) {
    return find("key", key).firstResult();
  }

  public static Uni<Proxy> findOrCreateByKey(Organization organization, String key, SnowflakeIdGenerator idGenerator) {
    return findByKey(key)
      .onItem().ifNotNull().transform(org -> org)
      .onItem().ifNull().switchTo(() -> {
        Proxy newProxy = new Proxy();
        newProxy.id = idGenerator.generate(Proxy.class.getSimpleName());
        newProxy.organization = organization;
        newProxy.key = key;
        // Set other default values if necessary
        return newProxy.persist();
      });
  }

}
