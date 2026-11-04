package com.resala.donation;

import com.resala.model.DonationType;
import java.time.LocalDate;

public record DonationFilter(Long donorId, DonationType type, LocalDate from, LocalDate to) {}
