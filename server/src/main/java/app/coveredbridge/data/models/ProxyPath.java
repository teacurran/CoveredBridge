package app.coveredbridge.data.models;

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
}

