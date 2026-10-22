package com.resala.donation;

import com.resala.model.DonationType;
import java.time.LocalDate;

// null means do not filter on that value - that stuff is optional
public record DonationFilter(Long donorId, DonationType type, LocalDate from, LocalDate to) {}
