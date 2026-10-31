package com.resala.beneficiary;

import com.resala.beneficiary.policy.ByNeedDistributionPolicy;
import com.resala.beneficiary.policy.DistributionPolicy;
import com.resala.model.Beneficiary;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BeneficiaryService {

  private final BeneficiaryRepository repository;
  private DistributionPolicy distributionPolicy;

  public BeneficiaryService(BeneficiaryRepository repository) {
    this(repository, new ByNeedDistributionPolicy());
  }

  public BeneficiaryService(BeneficiaryRepository repository, DistributionPolicy distributionPolicy) {
    this.repository = repository;
    this.distributionPolicy = Objects.requireNonNull(distributionPolicy, "Distribution policy must not be null");
  }

  public void setDistributionPolicy(DistributionPolicy distributionPolicy) {
    this.distributionPolicy = Objects.requireNonNull(distributionPolicy, "Distribution policy must not be null");
  }

  public DistributionPolicy getDistributionPolicy() {
    return distributionPolicy;
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

  // ── Strategy-based Distribution ───────────────────────────────────────────

  /**
   * Previews how available aid will be allocated under the active policy without persisting.
   *
   * @param totalItems total quantity of aid to distribute
   * @return map of active Beneficiary to allocated quantity
   */
  public Map<Beneficiary, Integer> previewDistribution(int totalItems) {
    if (totalItems <= 0) {
      throw new IllegalArgumentException("Total items must be greater than 0.");
    }
    List<Beneficiary> activeBeneficiaries = repository.findAll().stream()
        .filter(b -> b.getStatus() == Beneficiary.Status.ACTIVE)
        .toList();
    return distributionPolicy.allocate(activeBeneficiaries, totalItems);
  }

  /**
   * Executes and persists distributions according to the active strategy policy.
   *
   * @param itemDescription aid/item description
   * @param totalItems total quantity to allocate
   * @param donationId optional in-kind donation link
   * @param distributionDate date of distribution (defaults to today if null)
   * @param notes optional notes
   * @return list of newly created Distribution records
   */
  public List<Distribution> distributeByPolicy(
      String itemDescription,
      int totalItems,
      Long donationId,
      LocalDate distributionDate,
      String notes
  ) {
    if (itemDescription == null || itemDescription.isBlank()) {
      throw new IllegalArgumentException("Item description is required.");
    }
    if (totalItems <= 0) {
      throw new IllegalArgumentException("Total items must be greater than 0.");
    }
    if (distributionDate == null) {
      distributionDate = LocalDate.now();
    }

    Map<Beneficiary, Integer> allocation = previewDistribution(totalItems);
    List<Distribution> recorded = new ArrayList<>();

    String policyNote = (notes != null && !notes.isBlank())
        ? notes.trim() + " (" + distributionPolicy.getName() + ")"
        : "Allocated via " + distributionPolicy.getName();

    for (Map.Entry<Beneficiary, Integer> entry : allocation.entrySet()) {
      int qty = entry.getValue();
      if (qty > 0) {
        Distribution d = recordDistribution(
            entry.getKey().getId(),
            donationId,
            itemDescription.trim(),
            qty,
            distributionDate,
            policyNote
        );
        recorded.add(d);
      }
    }
    return recorded;
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
