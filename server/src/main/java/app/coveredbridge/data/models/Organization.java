package app.coveredbridge.data.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Organization extends DefaultPanacheEntityWithTimestamps {

  @Column(nullable = true, unique = true)
  public String key;

  public String name;

  @OneToMany(mappedBy = "organization")
  public List<Proxy> proxies = new ArrayList<>();
}
