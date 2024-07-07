package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;

import java.math.BigInteger;
import java.util.List;

@JsonRootName("organization")
public class OrganizationType {

  private String key;

  List<ProxyType> proxies;

}
