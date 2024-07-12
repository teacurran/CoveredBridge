package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonRootName("host")
public class HostType {

  private String name;
  private boolean isValidated;

  public HostType() {
    // default no-arg constructor
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean getIsValidated() {
    return isValidated;
  }

  public void setIsValidated(boolean isValidated) {
    this.isValidated = isValidated;
  }
}
