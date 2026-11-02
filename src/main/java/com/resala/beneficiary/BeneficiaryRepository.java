package com.resala.beneficiary;

import com.resala.model.Beneficiary;
import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository {
    Beneficiary save(Beneficiary beneficiary);

    Optional<Beneficiary> findById(long id);

    List<Beneficiary> findAll();

    void delete(long id);

    Distribution saveDistribution(Distribution distribution);

    List<Distribution> findDistributionsByBeneficiary(long beneficiaryId);

    List<Distribution> findAllDistributions();
}
