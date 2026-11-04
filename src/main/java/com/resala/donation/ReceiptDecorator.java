package com.resala.donation;

public abstract class ReceiptDecorator implements Receipt {

  protected final Receipt receipt;

  protected ReceiptDecorator(Receipt receipt) {
    this.receipt = receipt;
  }
}
