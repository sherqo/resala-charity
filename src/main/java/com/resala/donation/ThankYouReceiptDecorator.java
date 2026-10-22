package com.resala.donation;

public class ThankYouReceiptDecorator extends ReceiptDecorator {

  public ThankYouReceiptDecorator(Receipt receipt) {
    super(receipt);
  }

  public String render() {
    return receipt.render() + "\nThank you for supporting Resala.";
  }
}
