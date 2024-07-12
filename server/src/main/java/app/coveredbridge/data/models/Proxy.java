package app.coveredbridge.data.models;

import app.coveredbridge.services.SnowflakeIdGenerator;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="proxies",
  uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "key"}))
@NamedQuery(name = "Proxy.findByHostByName", query = """
  SELECT P
  FROM Proxy P
  JOIN P.hosts H
  JOIN P.organization
  LEFT JOIN Fetch P.paths Pa
  WHERE H.name = :host
  AND H.isValidated = true
  """)
public class Proxy extends DefaultPanacheEntityWithTimestamps {

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

  public static Uni<Proxy> findByHostByName(String host) {
    return find("#Proxy.findByHostByName",
      Parameters.with("host", host).map()
    ).firstResult();
  }

}
