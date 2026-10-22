package com.resala.donation;

import java.util.List;
import java.util.Optional;

// donation persistence contract.
public interface DonationRepository {
  Donation save(Donation donation);

  Optional<Donation> findById(long id);

  List<Donation> find(DonationFilter filter);

  long monetaryTotalForDonorYear(long donorId, int year);
}
