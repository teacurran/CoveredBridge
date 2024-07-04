package app.coveredbridge.data.models;

import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public class Host extends DefaultPanacheEntityWithTimestamps {
  public String name;

  @Column(nullable = false, columnDefinition="BOOLEAN DEFAULT false")
  public boolean isValidated;

  @ManyToOne(fetch = FetchType.EAGER)
  public Account account;

  @OneToMany(
    mappedBy = "host",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  public List<Path> paths = new ArrayList<>();

  public static Uni<Host> findByValidatedName(String value) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("name", value);
    parameters.put("validated", true);
    return find("name=:name AND isValidated=:validated", parameters).firstResult();
  }

}
