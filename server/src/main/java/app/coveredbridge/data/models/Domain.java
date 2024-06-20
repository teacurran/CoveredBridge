package app.coveredbridge.data.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Domain extends DefaultPanacheEntity {
  public String name;

  @Column(nullable = false, columnDefinition="BOOLEAN DEFAULT false")
  public boolean isValidated;
}
