package app.coveredbridge.data.models;

import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.util.HashMap;
import java.util.Map;

@Entity
public class Host extends DefaultPanacheEntityWithTimestamps {

  @ManyToOne
  public Proxy proxy;

  public String name;

  @Column(nullable = false, columnDefinition="BOOLEAN DEFAULT false")
  public boolean isValidated;

  @ManyToOne
  public Account account;

  public static Uni<Host> findByValidatedName(String value) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("name", value);
    parameters.put("validated", true);
    return find("name=:name AND isValidated=:validated", parameters).firstResult();
  }

}
