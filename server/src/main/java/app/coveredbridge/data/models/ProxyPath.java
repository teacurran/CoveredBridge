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
  public Proxy proxy;

  public String path;

  @Column(nullable = false)
  public BigDecimal rank;

  public String target;

  public static Uni<ProxyPath> findByProxyAndPath(Proxy proxy, String path) {
    return find("proxy = ?1 AND path = ?2", proxy, path).firstResult();
  }

  public static Uni<ProxyPath> findOrCreateByProxyAndPath(Proxy proxy, String path, SnowflakeIdGenerator idGenerator) {
    return findByProxyAndPath(proxy, path)
      .onItem().transformToUni(proxyPath -> {
        if (proxyPath != null) {
          return Uni.createFrom().item(proxyPath);
        }
        ProxyPath newPath = new ProxyPath();
        newPath.id = idGenerator.generate(ProxyPath.class.getSimpleName());
        newPath.rank = new BigDecimal(0);
        newPath.proxy = proxy;
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

