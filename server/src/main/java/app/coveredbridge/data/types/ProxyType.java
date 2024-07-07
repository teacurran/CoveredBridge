package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

@JsonRootName("proxy")
public class ProxyType {

  private String key;

  private List<HostType> hostTypes;
}
