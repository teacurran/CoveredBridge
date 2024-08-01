package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
@JsonRootName("account")
public class AccountType {
  String username;
  String password;
  String status;
  String groups[];
  List<EmailType> emails;
  List<PhoneNumberType> phoneNumbers;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String[] getGroups() {
    return groups;
  }

  public void setGroups(String[] groups) {
    this.groups = groups;
  }

  public List<EmailType> getEmails() {
    return emails;
  }

  public void setEmails(List<EmailType> emails) {
    this.emails = emails;
  }

  public List<PhoneNumberType> getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(List<PhoneNumberType> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }
}
