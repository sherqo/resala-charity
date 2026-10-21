package com.resala.donation;

// Base decorator
public abstract class ReceiptDecorator implements Receipt {

  protected final Receipt receipt;

  protected ReceiptDecorator(Receipt receipt) {
    this.receipt = receipt;
  }
}
