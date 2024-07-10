package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
@JsonRootName("config")
public class ConfigType {
  List<OrganizationType> organizations;

  public ConfigType() {
    // default no-arg constructor
  }

  public List<OrganizationType> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(List<OrganizationType> organizations) {
    this.organizations = organizations;
  }
}
