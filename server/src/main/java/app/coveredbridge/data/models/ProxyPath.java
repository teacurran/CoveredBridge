package app.coveredbridge.data.models;

import app.coveredbridge.data.types.ProxyPathType;
import app.coveredbridge.services.SnowflakeIdGenerator;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(name = "proxy_paths",
  uniqueConstraints = @UniqueConstraint(columnNames = {"proxy_id", "path"})
)
public class ProxyPath extends DefaultPanacheEntityWithTimestamps {

  @ManyToOne(fetch = FetchType.LAZY)
  public Site site;

  public String path;

  @Column(nullable = false)
  public BigDecimal rank;

  public String target;

  public static Uni<ProxyPath> findByProxyAndPath(Site site, String path) {
    return find("proxy = ?1 AND path = ?2", site, path).firstResult();
  }

  public static Uni<ProxyPath> findOrCreateByProxyAndPath(Site site, String path, SnowflakeIdGenerator idGenerator) {
    return findByProxyAndPath(site, path)
      .onItem().transformToUni(proxyPath -> {
        if (proxyPath != null) {
          return Uni.createFrom().item(proxyPath);
        }
        ProxyPath newPath = new ProxyPath();
        newPath.id = idGenerator.generate(ProxyPath.class.getSimpleName());
        newPath.rank = new BigDecimal(0);
        newPath.site = site;
        newPath.path = path;
        return newPath.persist();
      });
  }

  public Uni<ProxyPath> updateFromJson(ProxyPathType json) {
    this.path = json.getPath();
    this.target = json.getTarget();
    return this.persistAndFlush();
  }
}

