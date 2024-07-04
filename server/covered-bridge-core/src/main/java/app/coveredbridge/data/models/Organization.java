package app.coveredbridge.data.models;

import jakarta.persistence.Entity;

@Entity
public class Organization extends DefaultPanacheEntityWithTimestamps {
  public String name;
}
