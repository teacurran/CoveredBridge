package app.coveredbridge.data.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Account extends DefaultPanacheEntityWithTimestamps {

  @OneToMany(
    mappedBy = "account",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  public List<Host> hosts = new ArrayList<>();

}
