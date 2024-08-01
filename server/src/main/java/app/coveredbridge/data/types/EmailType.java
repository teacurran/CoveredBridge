package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonRootName("email")
public class EmailType {
  String email;
  Boolean isValidated;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Boolean getValidated() {
    return isValidated;
  }

  public void setValidated(Boolean validated) {
    isValidated = validated;
  }
}
