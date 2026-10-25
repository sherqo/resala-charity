package com.resala.controller;

import com.resala.beneficiary.BeneficiaryService;
import com.resala.beneficiary.Distribution;
import com.resala.model.Beneficiary;
import java.time.LocalDate;
import java.util.List;

/**
 * MVC Controller for the beneficiary module.
 * Translates view input into BeneficiaryService calls.
 * No I/O, no SQL.
 */
public class BeneficiaryController {

  private final BeneficiaryService service;

  public BeneficiaryController(BeneficiaryService service) {
    this.service = service;
  }

  public Beneficiary add(Beneficiary b) {
    return service.add(b);
  }

  public Beneficiary update(Beneficiary b) {
    return service.update(b);
  }

  public Beneficiary getBeneficiary(long id) {
    return service.getBeneficiary(id);
  }

  public List<Beneficiary> listBeneficiaries() {
    return service.findAll();
  }

  public void delete(long id) {
    service.delete(id);
  }

  public Distribution recordDistribution(
      long beneficiaryId,
      Long donationId,
      String itemDescription,
      int quantity,
      LocalDate distributionDate,
      String notes
  ) {
    return service.recordDistribution(beneficiaryId, donationId, itemDescription, quantity, distributionDate, notes);
  }

  public List<Distribution> getDistributionHistory(long beneficiaryId) {
    return service.getDistributionHistory(beneficiaryId);
  }

  public List<Distribution> listDistributions() {
    return service.getAllDistributions();
  }
}
