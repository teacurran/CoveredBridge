package app.coveredbridge.data.models;

import app.coveredbridge.services.SnowflakeIdGenerator;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "groups", indexes = {
  @Index(name = "uq_group_name_organization", columnList = "name, organization_id", unique = true)
})
public class Group extends DefaultPanacheEntityWithTimestamps {
  public String name;
  public String description;

  @ManyToOne(optional = false)
  public Organization organization;

  @ManyToMany
  @JoinTable(name = "permission_groups", joinColumns = { @JoinColumn(name = "permission_group_id") },
    inverseJoinColumns = { @JoinColumn(name = "permission_id") })
  public Set<Permission> permissions;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "account_groups", joinColumns = { @JoinColumn(name = "group_id") },
    inverseJoinColumns = { @JoinColumn(name = "account_id") })
  public Set<Account> accounts;

  public String toString() {
    return "PermissionGroup<" + this.id + ">";
  }

  public static Uni<Group> findByName(Organization organization, String name) {
    return find("organization = ?1 AND name = ?2", organization, name).firstResult();
  }

  public static Uni<Group> findOrCreateByName(Organization organization, String name, SnowflakeIdGenerator idGenerator) {
    return findByName(organization, name)
      .onItem().ifNotNull().transform(group -> group)
      .onItem().ifNull().switchTo(() -> {
        Group newGroup = new Group();
        newGroup.id = idGenerator.generate(Group.class.getSimpleName());
        newGroup.organization = organization;
        newGroup.name = name;
        return newGroup.persist();
      });
  }
}
