package com.resala.donation;

import com.resala.model.DonationType;
import com.resala.model.Donor;
import com.resala.notification.DonationObserver;
import com.resala.notification.DonationSubject;
import com.resala.payment.PaymentGateway;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

// business logic
public class DonationService implements DonationSubject {

  private final DonorRepository donors;
  private final DonationRepository donations;
  private final PaymentGateway paymentGateway;
  private final List<DonationObserver> observers = new ArrayList<>();

  public DonationService(DonorRepository donors, DonationRepository donations, PaymentGateway paymentGateway) {
    this.donors = donors;
    this.donations = donations;
    this.paymentGateway = paymentGateway;
  }

  public Donor saveDonor(Donor donor) {
    if (donor.getName() == null || donor.getName().isBlank()) throw new IllegalArgumentException(
      "Donor name is required"
    );
    if (donor.getType() == null) throw new IllegalArgumentException("Donor type is required");
    donor.setActive(true);
    return donors.save(donor);
  }

  public Donation record(DonationRequest request) {
    Donor donor = donors.findById(request.donorId()).orElseThrow(() -> new IllegalArgumentException("Donor not found"));
    if (!donor.isActive()) throw new IllegalArgumentException("Archived donors cannot make new donations");

    DonationRequest processed = request;
    if (
      request.type() == DonationType.ONLINE &&
      (request.referenceNumber() == null || request.referenceNumber().isBlank())
    ) {
      if (request.amountMinor() == null || request.amountMinor() <= 0) throw new IllegalArgumentException(
        "Online donation amount must be positive"
      );
      processed = new DonationRequest(
        request.donorId(),
        request.type(),
        request.amountMinor(),
        request.itemDescription(),
        request.quantity(),
        request.estimatedValueMinor(),
        paymentGateway.charge(request.amountMinor()),
        request.donationDate(),
        request.notes()
      );
    }

    Donation donation = new DonationFactory().create(processed);
    donations.save(donation);
    notifyObservers(donation, donor);
    return donation;
  }

  public List<Donor> findActiveDonors(String query) {
    return donors.findActive(query);
  }

  public List<Donation> findDonations(DonationFilter filter) {
    return donations.find(filter);
  }

  public Donation getDonation(long id) {
    return donations.findById(id).orElseThrow(() -> new IllegalArgumentException("Donation not found"));
  }

  public Donor getDonor(long id) {
    return donors.findById(id).orElseThrow(() -> new IllegalArgumentException("Donor not found"));
  }

  public void archiveDonor(long id) {
    donors.archive(id);
  }

  public String receipt(long donationId) {
    Donation donation = getDonation(donationId);
    Donor donor = getDonor(donation.getDonorId());
    Receipt receipt = new ThankYouReceiptDecorator(new BasicReceipt(donation, donor));
    if (donation.getType() != DonationType.IN_KIND) receipt = new TaxReceiptDecorator(receipt);
    return receipt.render();
  }

  public String yearlyStatement(long donorId, int year) {
    Donor donor = getDonor(donorId);
    if (year < 2000 || year > Year.now().getValue()) throw new IllegalArgumentException("Enter a valid year");
    long total = donations.monetaryTotalForDonorYear(donorId, year);
    return (
      "RESALA CHARITY\nYearly statement\nDonor: " +
      donor.getName() +
      "\nYear: " +
      year +
      "\nMonetary donations: " +
      BasicReceipt.money(total)
    );
  }

  public void addObserver(DonationObserver observer) {
    observers.add(observer);
  }

  public void removeObserver(DonationObserver observer) {
    observers.remove(observer);
  }

  private void notifyObservers(Donation donation, Donor donor) {
    for (DonationObserver observer : observers) observer.onDonationRecorded(donation, donor);
  }
}
