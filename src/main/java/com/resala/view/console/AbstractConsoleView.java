package com.resala.view.console;

import java.util.Scanner;

public abstract class AbstractConsoleView {

  protected static final String LINE = "----------------------------------------";

  protected final Scanner input;

  protected AbstractConsoleView(Scanner input) {
    this.input = input;
  }

  protected String readLine(String prompt) {
    System.out.print(prompt);
    return input.nextLine().trim();
  }

  protected String readLine() {
    return input.nextLine().trim();
  }

  protected String requiredLine(String prompt) {
    String value = readLine(prompt);
    if (value.isBlank()) throw new IllegalArgumentException("This field is required.");
    return value;
  }

  protected long readLong(String prompt) {
    return Long.parseLong(requiredLine(prompt));
  }

  protected int readInt(String prompt) {
    return Integer.parseInt(requiredLine(prompt));
  }

  protected String value(String label, String current) {
    String suffix = current == null ? "" : " (current: " + current + ")";
    System.out.print(label + suffix + ": ");
    String entered = input.nextLine().trim();
    return entered.isBlank() && current != null ? current : entered;
  }

  protected void printError(String message) {
    System.out.println("Error: " + message);
  }

  protected void printSuccess(String message) {
    System.out.println("✓ " + message);
  }

  protected String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  protected String truncate(String text, int max) {
    return text == null ? "" : text.length() > max ? text.substring(0, max) : text;
  }
}
