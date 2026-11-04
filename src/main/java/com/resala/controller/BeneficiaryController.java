package com.resala.controller;

import com.resala.beneficiary.BeneficiaryService;
import com.resala.beneficiary.Distribution;
import com.resala.beneficiary.policy.DistributionPolicy;
import com.resala.model.Beneficiary;
import com.resala.model.NeedType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

  public void setDistributionPolicy(DistributionPolicy policy) {
    service.setDistributionPolicy(policy);
  }

  public DistributionPolicy getDistributionPolicy() {
    return service.getDistributionPolicy();
  }

  public Map<Beneficiary, Integer> previewDistribution(int totalItems) {
    return service.previewDistribution(totalItems);
  }

  public List<Distribution> distributeByPolicy(
      String itemDescription,
      int totalItems,
      Long donationId,
      LocalDate distributionDate,
      String notes
  ) {
    return service.distributeByPolicy(itemDescription, totalItems, donationId, distributionDate, notes);
  }

  public List<NeedType> listNeedTypes() {
    return service.listNeedTypes();
  }

  public NeedType getNeedType(int id) {
    return service.getNeedType(id);
  }

  public NeedType addNeedType(String name) {
    return service.addNeedType(name);
  }

  public NeedType updateNeedType(int id, String name) {
    return service.updateNeedType(id, name);
  }

  public void deleteNeedType(int id) {
    service.deleteNeedType(id);
  }

  public int getImpactScore(long beneficiaryId) {
    return service.getImpactScore(beneficiaryId);
  }

  public void setImpactScore(long beneficiaryId, int score) {
    service.setImpactScore(beneficiaryId, score);
  }
}
