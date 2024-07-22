package app.coveredbridge.data.models;

import app.coveredbridge.data.types.AccountType;
import app.coveredbridge.services.SnowflakeIdGenerator;
import app.coveredbridge.utils.EncryptionUtil;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.runtime.JpaOperations;
import io.quarkus.panache.common.Parameters;
import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "accounts")
@NamedQuery(name = Account.QUERY_BY_ORG_AND_USERNAME, query = """
  SELECT a
  FROM Account a
  WHERE a.organization = :organization
  AND a.username = :username
  """)
public class Account extends DefaultPanacheEntityWithTimestamps {
  public static final String QUERY_BY_ORG_AND_USERNAME = "Account.findByOrgAndUsername";

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
    encryptFields();
    return super.persist();
  }

  @Override
  public <T extends PanacheEntityBase> Uni<T> persistAndFlush() {
    encryptFields();
    return super.persistAndFlush();
  }

  @WithSpan
  public void encryptFields() {
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
  }

  public static Uni<Account> findByOrgAndUsername(Organization organization, String username) {
    return find("#" + Account.QUERY_BY_ORG_AND_USERNAME,
      Parameters.with("organization", organization)
        .and("username", username)
    ).firstResult();
  }

  public static Uni<Account> findOrCreateByUsername(Organization organization, String username, SnowflakeIdGenerator idGenerator) {
    return findByOrgAndUsername(organization, username)
      .onItem().ifNotNull().transform(group -> group)
      .onItem().ifNull().switchTo(() -> {
        Account newItem = new Account();
        newItem.id = idGenerator.generate(Group.class.getSimpleName());
        newItem.organization = organization;
        newItem.username  = username;
        return newItem.persist();
      });
  }

  public Uni<Account> updateFromJson(AccountType accountFromJson) {
    this.username = accountFromJson.getUsername();
    this.password = accountFromJson.getPassword();
    return this.persistAndFlush();
  }

}
