package com.resala.beneficiary.policy;

import com.resala.model.Beneficiary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete Strategy — allocates aid based on need severity.
 * In this implementation, household size reflects need:
 * larger families are prioritized and receive a proportionally larger share of aid.
 */
public class ByNeedDistributionPolicy implements DistributionPolicy {

  @Override
  public String getName() {
    return "By Need (Proportional to Household Size)";
  }

  @Override
  public Map<Beneficiary, Integer> allocate(List<Beneficiary> candidates, int totalItems) {
    Map<Beneficiary, Integer> allocation = new LinkedHashMap<>();
    if (candidates == null || candidates.isEmpty()) {
      return allocation;
    }

    if (totalItems <= 0) {
      for (Beneficiary b : candidates) {
        allocation.put(b, 0);
      }
      return allocation;
    }

    int totalHouseholdMembers = candidates.stream()
        .mapToInt(b -> Math.max(1, b.getHouseholdSize()))
        .sum();

    int remaining = totalItems;
    for (Beneficiary b : candidates) {
      int size = Math.max(1, b.getHouseholdSize());
      int share = (int) Math.floor((double) size * totalItems / totalHouseholdMembers);
      allocation.put(b, share);
      remaining -= share;
    }

    // Allocate any leftover remainder to the largest households first
    if (remaining > 0) {
      List<Beneficiary> sortedByNeed = candidates.stream()
          .sorted((a, b) -> Integer.compare(b.getHouseholdSize(), a.getHouseholdSize()))
          .toList();

      for (Beneficiary b : sortedByNeed) {
        if (remaining == 0) {
          break;
        }
        allocation.put(b, allocation.get(b) + 1);
        remaining--;
      }
    }

    return allocation;
  }
}
