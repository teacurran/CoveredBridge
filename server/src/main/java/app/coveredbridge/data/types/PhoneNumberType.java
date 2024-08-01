package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonRootName("phone_number")
public class PhoneNumberType {
  String number;
  Boolean isValidated;

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public Boolean getValidated() {
    return isValidated;
  }

  public void setValidated(Boolean validated) {
    isValidated = validated;
  }
}
