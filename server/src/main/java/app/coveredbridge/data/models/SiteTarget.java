package app.coveredbridge.data.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "site_targets",
  uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "name"})
)
public class SiteTarget extends DefaultPanacheEntityWithTimestamps {

  @ManyToOne(fetch = FetchType.LAZY)
  public Site site;

  public String name;

  public String url;

}
