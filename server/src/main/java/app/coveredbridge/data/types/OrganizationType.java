package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
@JsonRootName("organization")
public class OrganizationType {

  private String key;

  List<ProxyType> proxies;

  public OrganizationType() {
    // default no-arg constructor
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<ProxyType> getProxies() {
    return proxies;
  }

  public void setProxies(List<ProxyType> proxies) {
    this.proxies = proxies;
  }
}
