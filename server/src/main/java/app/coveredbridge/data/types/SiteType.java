package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
@JsonRootName("site")
public class SiteType {

  private String key;

  private List<HostType> hosts;

  private List<SiteTargetType> targets;

  private List<SitePathType> paths;

  public SiteType() {
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

  public List<SitePathType> getPaths() {
    return paths;
  }

  public void setPaths(List<SitePathType> paths) {
    this.paths = paths;
  }

  public List<SiteTargetType> getTargets() {
    return targets;
  }

  public void setTargets(List<SiteTargetType> targets) {
    this.targets = targets;
  }
}
