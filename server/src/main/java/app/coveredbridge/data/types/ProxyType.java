package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
@JsonRootName("proxy")
public class ProxyType {

  private String key;

  private List<HostType> hosts;

  public ProxyType() {
    // default no-arg constructor
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<HostType> getHosts() {
    return hosts;
  }

  public void setHosts(List<HostType> hosts) {
    this.hosts = hosts;
  }
}
