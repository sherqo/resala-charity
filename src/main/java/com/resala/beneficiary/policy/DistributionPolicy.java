package com.resala.beneficiary.policy;

import com.resala.model.Beneficiary;
import java.util.List;
import java.util.Map;

/**
 * Strategy interface for aid distribution algorithms.
 * Each concrete policy implements a different allocation strategy
 * (e.g., by need, equal, or FIFO).
 */
public interface DistributionPolicy {

  /**
   * Calculates the allocation of available items across candidate beneficiaries.
   *
   * @param candidates list of active beneficiaries eligible for aid
   * @param totalItems total quantity of items/aid available for distribution
   * @return ordered map of Beneficiary to the allocated quantity (>= 0)
   */
  Map<Beneficiary, Integer> allocate(List<Beneficiary> candidates, int totalItems);

  /**
   * Returns the display name of this policy.
   */
  String getName();
}
