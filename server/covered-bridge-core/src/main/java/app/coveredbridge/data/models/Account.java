package app.coveredbridge.data.models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
public class Account extends DefaultPanacheEntityWithTimestamps {
  public String email;
  public AccountStatus status;

  @ManyToOne(fetch = FetchType.EAGER)
  public Organization organization;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "account_permission_groups", joinColumns = { @JoinColumn(name = "account_id") },
    inverseJoinColumns = { @JoinColumn(name = "permission_group_id") })
  public Set<PermissionGroup> permissionGroups;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "account_permissions", joinColumns = { @JoinColumn(name = "account_id") },
    inverseJoinColumns = { @JoinColumn(name = "permission_id") })
  public Set<Permission> permissions;

  @OneToMany(
    mappedBy = "account",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  public List<Host> hosts = new ArrayList<>();

  public enum AccountStatus {
    ACTIVE(1),
    DISABLED(2),
    SUSPENDED(3),
    BANNED(4);

    private final int value;
    private AccountStatus(int value) {
      this.value = value;
    }

    public static AccountStatus fromValue(int id) {
      for (AccountStatus item : AccountStatus.values()) {
        if (item.getValue() == id) {
          return item;
        }
      }
      return null;
    }

    public int getValue() {
      return value;
    }

  }

}
