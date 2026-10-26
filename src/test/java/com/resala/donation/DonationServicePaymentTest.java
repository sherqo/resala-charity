package com.resala.donation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.resala.model.DonationType;
import com.resala.model.Donor;
import com.resala.payment.CashPaymentMethod;
import com.resala.payment.Payment;
import com.resala.payment.PaymentRepository;
import com.resala.payment.PaymentStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DonationServicePaymentTest {

  private static final long DONOR_ID = 7L;
  private static final long AMOUNT = 25_000L;

  private final FakeDonorRepository donors = new FakeDonorRepository();
  private final FakeDonationRepository donations = new FakeDonationRepository();
  private final FakePaymentRepository payments = new FakePaymentRepository();
  private final DonationService service = new DonationService(donors, donations, payments);

  @Test
  void rejectsOnlineDonationWhenReferenceHasNoPayment() {
    assertThrows(IllegalArgumentException.class,
        () -> service.record(online("unknown-ref", DONOR_ID, AMOUNT)));
    assertEquals(0, donations.saved.size());
  }

  @Test
  void rejectsOnlineDonationWhenPaymentIsNotPaid() {
    payments.save(pending("ref-1", DONOR_ID, AMOUNT));

    assertThrows(IllegalArgumentException.class,
        () -> service.record(online("ref-1", DONOR_ID, AMOUNT)));
    assertEquals(0, donations.saved.size());
  }

  @Test
  void rejectsOnlineDonationWhenPaymentBelongsToAnotherDonor() {
    payments.save(paid("ref-1", DONOR_ID + 1, AMOUNT));

    assertThrows(IllegalArgumentException.class,
        () -> service.record(online("ref-1", DONOR_ID, AMOUNT)));
    assertEquals(0, donations.saved.size());
  }

  @Test
  void rejectsOnlineDonationWhenPaymentAmountDiffers() {
    payments.save(paid("ref-1", DONOR_ID, AMOUNT + 1));

    assertThrows(IllegalArgumentException.class,
        () -> service.record(online("ref-1", DONOR_ID, AMOUNT)));
    assertEquals(0, donations.saved.size());
  }

  @Test
  void recordsAndNotifiesOnlineDonationOnlyForMatchingCompletedPayment() {
    payments.save(paid("ref-1", DONOR_ID, AMOUNT));
    List<Donation> notified = new ArrayList<>();
    service.addObserver((donation, donor) -> notified.add(donation));

    Donation donation = service.record(online("ref-1", DONOR_ID, AMOUNT));

    assertEquals(1, donations.saved.size());
    assertEquals(donation, donations.saved.get(0));
    assertEquals(List.of(donation), notified);
  }

  @Test
  void recordConfirmedAppliesTheSamePaymentChecks() {
    payments.save(paid("ref-1", DONOR_ID, AMOUNT));
    List<Donation> notified = new ArrayList<>();
    service.addObserver((donation, donor) -> notified.add(donation));

    Donation donation = service.recordConfirmed(online("ref-1", DONOR_ID, AMOUNT));

    assertEquals(1, donations.saved.size());
    assertEquals(donation, donations.saved.get(0));
    assertEquals(List.of(), notified);
  }

  @Test
  void recordConfirmedRejectsUnpaidReference() {
    assertThrows(IllegalArgumentException.class,
        () -> service.recordConfirmed(online("unknown-ref", DONOR_ID, AMOUNT)));
    assertEquals(0, donations.saved.size());
  }

  @Test
  void checkDonationsDoNotRequireAPayment() {
    DonationRequest request = new DonationRequest(
        DONOR_ID, DonationType.CHECK, AMOUNT, null, null, null,
        "check-123", LocalDate.now(), null);

    Donation donation = service.record(request);

    assertEquals(1, donations.saved.size());
    assertEquals(donation, donations.saved.get(0));
  }

  private DonationRequest online(String reference, long donorId, long amountMinor) {
    return new DonationRequest(
        donorId, DonationType.ONLINE, amountMinor, null, null, null,
        reference, LocalDate.now(), null);
  }

  private Payment pending(String reference, long donorId, long amountMinor) {
    Payment payment = Payment.createPending(
        "DONOR", donorId, amountMinor, "EGP", new CashPaymentMethod(), reference, null);
    payment.setId(nextPaymentId());
    return payment;
  }

  private Payment paid(String reference, long donorId, long amountMinor) {
    Payment payment = pending(reference, donorId, amountMinor);
    payment.markPaid("DONATION", 1L);
    return payment;
  }

  private long nextPaymentId() {
    return payments.byKey.size() + 1L;
  }

  private static final class FakeDonorRepository implements DonorRepository {
    @Override public Donor save(Donor donor) { return donor; }
    @Override public Optional<Donor> findById(long id) {
      return id == DONOR_ID
          ? Optional.of(new Donor(DONOR_ID, "Donor", Donor.Type.INDIVIDUAL, null, null, true))
          : Optional.empty();
    }
    @Override public List<Donor> findActive(String query) { return List.of(); }
    @Override public void archive(long id) {}
  }

  private static final class FakeDonationRepository implements DonationRepository {
    private final List<Donation> saved = new ArrayList<>();

    @Override public Donation save(Donation donation) {
      donation.setId(saved.size() + 1L);
      saved.add(donation);
      return donation;
    }
    @Override public Optional<Donation> findById(long id) {
      return saved.stream().filter(d -> d.getId() == id).findFirst();
    }
    @Override public List<Donation> find(DonationFilter filter) { return saved; }
    @Override public long monetaryTotalForDonorYear(long donorId, int year) { return 0; }
  }

  private static final class FakePaymentRepository implements PaymentRepository {
    private final Map<String, Payment> byKey = new HashMap<>();

    @Override public Payment save(Payment payment) {
      byKey.put(payment.getIdempotencyKey(), payment);
      return payment;
    }
    @Override public Optional<Payment> findById(long id) {
      return byKey.values().stream().filter(p -> p.getId() == id).findFirst();
    }
    @Override public Optional<Payment> findByIdempotencyKey(String key) {
      return Optional.ofNullable(byKey.get(key));
    }
    @Override public List<Payment> findByPayer(String payerType, long payerId) {
      return byKey.values().stream()
          .filter(p -> p.getPayerType().equals(payerType) && p.getPayerId() == payerId)
          .toList();
    }
    @Override public List<Payment> findByStatus(PaymentStatus status) {
      return byKey.values().stream().filter(p -> p.getStatus() == status).toList();
    }
  }
}
