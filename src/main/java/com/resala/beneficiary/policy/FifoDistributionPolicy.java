package com.resala.beneficiary.policy;

import com.resala.model.Beneficiary;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete Strategy — allocates aid sequentially in order of registration (FIFO).
 * Beneficiaries registered earlier (lowest ID) receive aid first up to a specified quota.
 */
public class FifoDistributionPolicy implements DistributionPolicy {

  private final int quotaPerBeneficiary;

  public FifoDistributionPolicy() {
    this(1);
  }

  public FifoDistributionPolicy(int quotaPerBeneficiary) {
    this.quotaPerBeneficiary = Math.max(1, quotaPerBeneficiary);
  }

  public int getQuotaPerBeneficiary() {
    return quotaPerBeneficiary;
  }

  @Override
  public String getName() {
    return "FIFO (First-In, First-Out by Registration, quota=" + quotaPerBeneficiary + ")";
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

    // Sort by ID ascending (earliest registered first)
    List<Beneficiary> sorted = candidates.stream()
        .sorted(Comparator.comparingLong(Beneficiary::getId))
        .toList();

    int remaining = totalItems;
    for (Beneficiary b : sorted) {
      if (remaining <= 0) {
        allocation.put(b, 0);
      } else {
        int granted = Math.min(quotaPerBeneficiary, remaining);
        allocation.put(b, granted);
        remaining -= granted;
      }
    }

    return allocation;
  }
}
