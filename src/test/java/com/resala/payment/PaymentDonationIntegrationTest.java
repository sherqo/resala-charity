package com.resala.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.resala.core.TransactionManager;
import com.resala.donation.Donation;
import com.resala.donation.DonationPaymentSettlement;
import com.resala.donation.DonationRequest;
import com.resala.donation.DonationService;
import com.resala.model.DonationType;
import com.resala.model.Donor;
import com.resala.persistence.sqlite.SqliteDonationRepository;
import com.resala.persistence.sqlite.SqliteDonorRepository;
import com.resala.persistence.sqlite.SqlitePaymentRepository;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentDonationIntegrationTest {

  @Test
  void recordsDonationOnlyWhenStoredPaymentIsPaidAndMatches() throws Exception {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
      runSchema(conn);
      Donor donor = new Donor(0, "Donor", Donor.Type.INDIVIDUAL, null, null, true);
      donor = new SqliteDonorRepository(conn).save(donor);

      SqlitePaymentRepository payments = new SqlitePaymentRepository(conn);
      Payment payment = Payment.createPending(
          "DONOR", donor.getId(), 5_000L, "EGP", new CashPaymentMethod(), "ref-1", null);
      payments.save(payment);
      DonationService service = new DonationService(
          new SqliteDonorRepository(conn),
          new SqliteDonationRepository(conn),
          payments);

      DonationRequest request = new DonationRequest(
          donor.getId(), DonationType.ONLINE, 5_000L, null, null, null,
          "ref-1", LocalDate.now(), null);
      assertThrows(IllegalArgumentException.class, () -> service.record(request));

      payment.markPaid("DONATION", 1L);
      payments.save(payment);

      Donation donation = service.record(request);
      assertEquals(5_000L, donation.getAmountMinor());
    }
  }

  @Test
  void paymentServiceSettlesOnlineDonationAfterGatewayApproval() throws Exception {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
      runSchema(conn);
      Donor donor = new SqliteDonorRepository(conn).save(
          new Donor(0, "Donor", Donor.Type.INDIVIDUAL, null, null, true));
      SqlitePaymentRepository payments = new SqlitePaymentRepository(conn);
      DonationService donations = new DonationService(
          new SqliteDonorRepository(conn),
          new SqliteDonationRepository(conn),
          payments);
      List<Donation> notified = new ArrayList<>();
      donations.addObserver((donation, donor1) -> notified.add(donation));

      PaymentService service = new PaymentService(
          payments,
          new FakePaymentGatewayAdapter(new FakePaymentProvider(0)),
          new TransactionManager(conn),
          new DonationPaymentSettlement(donations));

      PaymentOutcome outcome = service.pay(new PaymentRequest(
          "pay-1", "DONOR", donor.getId(), 5_001L, "EGP",
          new CardPaymentMethod(), "Online donation", null));

      assertTrue(outcome.isPaid());
      assertNotNull(outcome.referenceId());
      Donation donation = donations.getDonation(outcome.referenceId());
      assertEquals(5_001L, donation.getAmountMinor());
      assertEquals(DonationType.ONLINE, donation.getType());
      assertEquals(1, notified.size());
      assertEquals(donation.getId(), notified.get(0).getId());

      Payment stored = payments.findByIdempotencyKey("pay-1").orElseThrow();
      assertEquals(PaymentStatus.PAID, stored.getStatus());
      assertEquals("DONATION", stored.getReferenceType());
      assertEquals(donation.getId(), stored.getReferenceId());
    }
  }

  @Test
  void insertsAnAlreadyPaidPaymentWithItsReference() throws Exception {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
      runSchema(conn);
      SqlitePaymentRepository payments = new SqlitePaymentRepository(conn);
      Payment paid = Payment.createPending(
          "DONOR", 7L, 5_000L, "EGP", new CashPaymentMethod(), "ref-paid", null);
      paid.markPaid("DONATION", 1L);

      payments.save(paid);

      Payment stored = payments.findByIdempotencyKey("ref-paid").orElseThrow();
      assertEquals(PaymentStatus.PAID, stored.getStatus());
      assertEquals("DONATION", stored.getReferenceType());
      assertEquals(1L, stored.getReferenceId());
    }
  }

  private void runSchema(Connection conn) throws Exception {
    String script;
    try (InputStream in = getClass().getResourceAsStream("/schema.sql")) {
      script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    for (String sql : script.replaceAll("(?m)--.*$", "").split(";")) {
      if (sql.isBlank()) continue;
      try (Statement st = conn.createStatement()) {
        st.execute(sql.trim());
      }
    }
  }
}
