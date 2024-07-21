package app.coveredbridge.data.models.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MaxIntDto {
  public final int maxValue;

  public MaxIntDto(int maxValue) {
    this.maxValue = maxValue;
  }

  public MaxIntDto(Integer maxValue) {
    this.maxValue = maxValue;
  }
}
