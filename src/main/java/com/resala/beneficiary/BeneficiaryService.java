package com.resala.beneficiary;

import com.resala.model.Beneficiary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BeneficiaryService {

  private final BeneficiaryRepository repository;

  public BeneficiaryService(BeneficiaryRepository repository) {
    this.repository = repository;
  }

  public Beneficiary add(Beneficiary b) {
    validate(b);
    return repository.save(b);
  }

  public Beneficiary update(Beneficiary b) {
    if (b.getId() <= 0) {
      throw new IllegalArgumentException("Invalid beneficiary ID");
    }
    repository.findById(b.getId()).orElseThrow(() -> new IllegalArgumentException("Beneficiary not found: #" + b.getId()));
    validate(b);
    return repository.save(b);
  }

  public List<Beneficiary> findAll() {
    return repository.findAll();
  }

  public Optional<Beneficiary> findById(long id) {
    return repository.findById(id);
  }

  public Beneficiary getBeneficiary(long id) {
    return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Beneficiary not found: #" + id));
  }

  public void delete(long id) {
    repository.delete(id);
  }

  // ── Distribution Logic (B3 & B4) ──────────────────────────────────────────

  public Distribution recordDistribution(
      long beneficiaryId,
      Long donationId,
      String itemDescription,
      int quantity,
      LocalDate distributionDate,
      String notes
  ) {
    Beneficiary b = getBeneficiary(beneficiaryId);
    if (b.getStatus() == Beneficiary.Status.CLOSED) {
      throw new IllegalStateException("Cannot distribute to a CLOSED beneficiary (" + b.getName() + ").");
    }
    if (itemDescription == null || itemDescription.isBlank()) {
      throw new IllegalArgumentException("Item description is required.");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than 0.");
    }
    if (distributionDate == null) {
      distributionDate = LocalDate.now();
    }

    Distribution d = new Distribution(0, beneficiaryId, donationId, itemDescription.trim(), quantity, distributionDate, notes);
    return repository.saveDistribution(d);
  }

  public List<Distribution> getDistributionHistory(long beneficiaryId) {
    getBeneficiary(beneficiaryId); // ensures beneficiary exists
    return repository.findDistributionsByBeneficiary(beneficiaryId);
  }

  public List<Distribution> getAllDistributions() {
    return repository.findAllDistributions();
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private void validate(Beneficiary b) {
    if (b.getName() == null || b.getName().isBlank()) {
      throw new IllegalArgumentException("Beneficiary name is required.");
    }
    if (b.getHouseholdSize() <= 0) {
      throw new IllegalArgumentException("Household size must be at least 1.");
    }
    if (b.getStatus() == null) {
      b.setStatus(Beneficiary.Status.ACTIVE);
    }
  }
}
