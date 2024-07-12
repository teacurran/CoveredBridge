package app.coveredbridge.data.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "permission_groups")
public class PermissionGroup extends DefaultPanacheEntityWithTimestamps {
  public String code;
  public String name;
  public String description;

  @ManyToMany
  @JoinTable(name = "permission_group_permissions", joinColumns = { @JoinColumn(name = "permission_group_id") },
    inverseJoinColumns = { @JoinColumn(name = "permission_id") })
  public Set<Permission> permissions;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "account_permission_groups", joinColumns = { @JoinColumn(name = "permission_group_id") },
    inverseJoinColumns = { @JoinColumn(name = "account_id") })
  public Set<Account> accounts;

  public String toString() {
    return "PermissionGroup<" + this.id + ">";
  }
}
