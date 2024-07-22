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
  @JoinTable(name = "permission_groups", joinColumns = { @JoinColumn(name = "permission_id") },
    inverseJoinColumns = { @JoinColumn(name = "group_id") })
  public Set<Group> groups;

  @ManyToMany
  @JoinTable(name = "account_permissions", joinColumns = { @JoinColumn(name = "permission_id") },
    inverseJoinColumns = { @JoinColumn(name = "account_id") })
  public Set<Account> accounts;

  public String toString() {
    return "Permission<" + this.id + ">";
  }

}
