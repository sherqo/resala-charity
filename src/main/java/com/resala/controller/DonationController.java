package com.resala.controller;

import com.resala.donation.Donation;
import com.resala.donation.DonationFilter;
import com.resala.donation.DonationRequest;
import com.resala.donation.DonationService;
import com.resala.model.Donor;
import java.util.List;

public class DonationController {

  private final DonationService service;

  public DonationController(DonationService service) {
    this.service = service;
  }

  public Donor saveDonor(Donor donor) {
    return service.saveDonor(donor);
  }

  public void archiveDonor(long id) {
    service.archiveDonor(id);
  }

  public Donor getDonor(long id) {
    return service.getDonor(id);
  }

  public List<Donor> findDonors(String query) {
    return service.findActiveDonors(query);
  }

  public Donation recordDonation(DonationRequest request) {
    return service.record(request);
  }

  public List<Donation> findDonations(DonationFilter filter) {
    return service.findDonations(filter);
  }

  public String receipt(long donationId) {
    return service.receipt(donationId);
  }

  public String yearlyStatement(long donorId, int year) {
    return service.yearlyStatement(donorId, year);
  }
}
