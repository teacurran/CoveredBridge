package app.coveredbridge.data.models;

import app.coveredbridge.data.types.AccountType;
import app.coveredbridge.services.SnowflakeIdGenerator;
import app.coveredbridge.utils.EncryptionUtil;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;
import org.jboss.logging.Logger;

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
  private static final Logger LOGGER = Logger.getLogger(Account.class);

  public static final String QUERY_BY_ORG_AND_USERNAME = "Account.findByOrgAndUsername";

  public String username;
  public String passwordEncrypted;
  public String passwordSalt;

  @Enumerated(EnumType.STRING)
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
    ACTIVE,
    DISABLED,
    SUSPENDED,
    BANNED;

    public static AccountStatus fromName(String name) {
      for (AccountStatus item : AccountStatus.values()) {
        if (item.name().equalsIgnoreCase(name)) {
          return item;
        }
      }
      return null;
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

  public static Uni<Account> createOrUpdateFromJson(Organization organization, AccountType accountJson, SnowflakeIdGenerator idGenerator) {
    return findByOrgAndUsername(organization, accountJson.getUsername())
      .onItem()
      .call(item -> {
      if (item == null) {
        item = new Account();
        item.id = idGenerator.generate(Account.class.getSimpleName());
        item.organization = organization;
        item.username = accountJson.getUsername();
        if (accountJson.getPassword() != null) {
          item.password = accountJson.getPassword();
        }
      }

      item.status = AccountStatus.fromName(accountJson.getStatus());
      if (item.status == null) {
        LOGGER.warn("Status '" + accountJson.getStatus() + "' not valid for account:" + item.id + ". setting to disabled");
        item.status = AccountStatus.DISABLED;
      }

      return item.persistAndFlush();
    });
  }

  public Uni<Account> updateFromJson(AccountType accountFromJson) {
    this.username = accountFromJson.getUsername();
    this.password = accountFromJson.getPassword();
    return this.persistAndFlush();
  }

}
