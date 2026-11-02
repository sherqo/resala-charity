package com.resala.volunteer;

import com.resala.model.Volunteer;
import java.time.LocalDate;
import java.util.Locale;

/** Builds the plain-text volunteer certificate. */
public class CertificateGenerator {

  public static final double REQUIRED_HOURS = 20.0;

  public String generate(Volunteer volunteer, VolunteerSummary summary) {
    if (summary.getTotalHours() < REQUIRED_HOURS) {
      throw new IllegalStateException(
        String.format(
          Locale.US,
          "Volunteer needs at least %.0f hours. Current total: %.1f",
          REQUIRED_HOURS,
          summary.getTotalHours()
        )
      );
    }

    return String.join(
      "\n",
      "RESALA CHARITY - VOLUNTEER CERTIFICATE",
      "----------------------------------------",
      "This certificate is proudly presented to",
      volunteer.getName(),
      "",
      String.format(Locale.US, "For completing %.1f volunteer hours.", summary.getTotalHours()),
      "Issued on: " + LocalDate.now(),
      "----------------------------------------",
      "Authorized signature"
    );
  }
}
