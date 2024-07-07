package app.coveredbridge.data.types;

import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

@JsonRootName("config")
public class ConfigType {
  List<OrganizationType> organizations;
}
