package app.coveredbridge.data.models;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@MappedSuperclass
public abstract class DefaultPanacheEntityWithTimestamps extends PanacheEntityBase {

  @Id
  @Column(columnDefinition = "CHAR(13)", length = 13)
  public String id;

  @CreationTimestamp
  @Column(name="created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name="updated_at")
  private Instant updatedAt;

  public String toString() {
    String var10000 = this.getClass().getSimpleName();
    return var10000 + "<" + this.id + ">";
  }

}
