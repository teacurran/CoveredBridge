package app.coveredbridge.data.models;

import app.coveredbridge.data.models.dto.MaxIntDto;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
public class Server extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  public int instanceNumber;

  public LocalDateTime seen;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
  public boolean isShutdown;

  public static Uni<MaxIntDto> findMaxInstanceId() {
    return find("select coalesce(max(instanceNumber), 0) as maxValue from Server").project(MaxIntDto.class).firstResult();
  }

  public static Uni<Server> findFirstUnusedServer() {
    return find("isShutdown=true").firstResult();
  }

  public static Uni<Server> byId(Long id) {
    return findById(id);
  }

}
