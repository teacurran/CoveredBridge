package app.coveredbridge.data.models;

import app.coveredbridge.utils.EncryptionUtil;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.runtime.JpaOperations;
import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "accounts")
public class Account extends DefaultPanacheEntityWithTimestamps {
  public String username;
  public String passwordEncrypted;
  public String passwordSalt;
  public AccountStatus status;

  @Transient
  public String password;

  @ManyToOne(fetch = FetchType.EAGER)
  public Organization organization;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "account_groups", joinColumns = { @JoinColumn(name = "account_id") },
    inverseJoinColumns = { @JoinColumn(name = "group_id") })
  public Set<Group> groups;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "account_permissions", joinColumns = { @JoinColumn(name = "account_id") },
    inverseJoinColumns = { @JoinColumn(name = "permission_id") })
  public Set<Permission> permissions;

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

  @Override
  public <T extends PanacheEntityBase> Uni<T> persist() {
    if (this.password != null) {
      byte[] saltBytes = null;
      if (this.passwordSalt == null) {
        saltBytes = EncryptionUtil.generateSalt16Byte();
      } else {
        saltBytes = EncryptionUtil.base64Decode(this.passwordSalt);
      }

      this.passwordEncrypted = EncryptionUtil.base64Encode(EncryptionUtil.generateArgon2Sensitive(this.password, saltBytes));
      this.password = null;
      this.passwordSalt = EncryptionUtil.base64Encode(saltBytes);
    }

    return super.persist();
  }

  @Override
  public <T extends PanacheEntityBase> Uni<T> persistAndFlush() {
    return super.persistAndFlush();
  }

}
