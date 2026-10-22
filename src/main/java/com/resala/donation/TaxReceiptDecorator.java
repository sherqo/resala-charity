package com.resala.donation;

public class TaxReceiptDecorator extends ReceiptDecorator {

  public TaxReceiptDecorator(Receipt receipt) {
    super(receipt);
  }

  public String render() {
    return receipt.render() + "\nThis receipt may be used for your tax records.";
  }
}
