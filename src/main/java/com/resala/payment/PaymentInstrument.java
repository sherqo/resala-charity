package com.resala.payment;

/**
 * Raw details captured by the UI before they are validated and tokenized by a
 * {@link PaymentMethod} strategy. Implementations carry card or wallet input;
 * cash has no instrument.
 */
public interface PaymentInstrument {}
