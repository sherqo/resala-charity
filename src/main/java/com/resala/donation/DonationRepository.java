package com.resala.donation;

import java.util.List;
import java.util.Optional;

// donation persistence contract.
public interface DonationRepository {
  Donation save(Donation donation);

  Optional<Donation> findById(long id);

  List<Donation> find(DonationFilter filter);

  long monetaryTotalForDonorYear(long donorId, int year);

  void delete(long id);

  Donation update(DonationRequest request, long donationId);

  /**
   * Finds an existing donation by its reference number. Used to prevent duplicate
   * reference numbers across donations.
   */
  Optional<Donation> findByReferenceNumber(String referenceNumber);
}
