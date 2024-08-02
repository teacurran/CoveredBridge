package app.coveredbridge.data.models;

import app.coveredbridge.data.types.SitePathType;
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
@Table(name = "site_paths",
  uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "path"})
)
public class SitePath extends DefaultPanacheEntityWithTimestamps {

  @ManyToOne(fetch = FetchType.LAZY)
  public Site site;

  public String path;

  @Column(nullable = false)
  public BigDecimal rank;

  public String target;

  public static Uni<SitePath> findBySiteAndPath(Site site, String path) {
    return find("site = ?1 AND path = ?2", site, path).firstResult();
  }

  public static Uni<SitePath> findOrCreateBySiteAndPath(Site site, String path, SnowflakeIdGenerator idGenerator) {
    return findBySiteAndPath(site, path)
      .onItem().transformToUni(proxyPath -> {
        if (proxyPath != null) {
          return Uni.createFrom().item(proxyPath);
        }
        SitePath newPath = new SitePath();
        newPath.id = idGenerator.generate(SitePath.class.getSimpleName());
        newPath.rank = new BigDecimal(0);
        newPath.site = site;
        newPath.path = path;
        return newPath.persist();
      });
  }

  public Uni<SitePath> updateFromJson(SitePathType json) {
    this.path = json.getPath();
    this.target = json.getTarget();
    return this.persistAndFlush();
  }
}

