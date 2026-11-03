package com.resala.beneficiary.policy;

import com.resala.model.Beneficiary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete Strategy — divides available aid equally among all eligible beneficiaries.
 */
public class EqualDistributionPolicy implements DistributionPolicy {

  @Override
  public String getName() {
    return "Equal (Even Distribution)";
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

    int baseAmount = totalItems / candidates.size();
    int remainder = totalItems % candidates.size();

    for (Beneficiary b : candidates) {
      int extra = (remainder-- > 0) ? 1 : 0;
      allocation.put(b, baseAmount + extra);
    }

    return allocation;
  }
}
