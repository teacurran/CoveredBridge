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
@Table(name="sites",
  uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "key"}))
@NamedQuery(name = "Proxy.findByHostByName", query = """
  SELECT P
  FROM Site P
    JOIN P.hosts PH
    JOIN Fetch P.organization
    LEFT JOIN Fetch P.paths PP
  WHERE PH.name = :host
    AND PH.isValidated = true
  ORDER BY PP.rank DESC
  """)
public class Site extends DefaultPanacheEntityWithTimestamps {

  @ManyToOne
  public Organization organization;

  public String key;

  @OneToMany(
    mappedBy = "site",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  public List<ProxyHost> hosts = new ArrayList<>();

  @OneToMany(
    mappedBy = "site",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  public List<ProxyPath> paths = new ArrayList<>();

  public static Uni<Site> findByKey(String key) {
    return find("key", key).firstResult();
  }

  public static Uni<Site> findOrCreateByKey(Organization organization, String key, SnowflakeIdGenerator idGenerator) {
    return findByKey(key)
      .onItem().ifNotNull().transform(org -> org)
      .onItem().ifNull().switchTo(() -> {
        Site newSite = new Site();
        newSite.id = idGenerator.generate(Site.class.getSimpleName());
        newSite.organization = organization;
        newSite.key = key;
        // Set other default values if necessary
        return newSite.persist();
      });
  }

  public static Uni<Site> findByHostByName(String host) {
    return find("#Proxy.findByHostByName",
      Parameters.with("host", host).map()
    ).firstResult();
  }

}
