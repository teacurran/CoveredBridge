package app.coveredbridge.data.models;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "permissions")
public class Permission extends DefaultPanacheEntityWithTimestamps {
  public String name;
  public String description;

  @ManyToMany
  @JoinTable(name = "permission_group_permissions", joinColumns = { @JoinColumn(name = "permission_id") },
    inverseJoinColumns = { @JoinColumn(name = "permission_group_id") })
  public Set<PermissionGroup> permissionGroups;

  @ManyToMany
  @JoinTable(name = "account_permissions", joinColumns = { @JoinColumn(name = "permission_id") },
    inverseJoinColumns = { @JoinColumn(name = "account_id") })
  public Set<Account> accounts;

  public String toString() {
    return "Permission<" + this.id + ">";
  }

}
