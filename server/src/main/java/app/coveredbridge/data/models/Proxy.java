package app.coveredbridge.data.models;

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

}
