package com.resala.beneficiary.policy;

import com.resala.model.Beneficiary;
import java.util.List;
import java.util.Map;

public interface DistributionPolicy {

  Map<Beneficiary, Integer> allocate(List<Beneficiary> candidates, int totalItems);

  String getName();
}
