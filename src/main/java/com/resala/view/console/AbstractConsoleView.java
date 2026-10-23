package com.resala.view.console;

import java.util.Scanner;

/**
 * Shared I/O helpers for all console views.
 * Every ConsoleXxxView extends this class; no view duplicates these utilities.
 */
public abstract class AbstractConsoleView {

  protected static final String LINE = "----------------------------------------";

  protected final Scanner input;

  protected AbstractConsoleView(Scanner input) {
    this.input = input;
  }

  // ── Input helpers ──────────────────────────────────────────────────────────

  /** Prints prompt, returns trimmed line. */
  protected String readLine(String prompt) {
    System.out.print(prompt);
    return input.nextLine().trim();
  }

  /** Reads the next line without printing a prompt (used after switch-reading). */
  protected String readLine() {
    return input.nextLine().trim();
  }

  /** Reads a required non-blank line; throws if blank. */
  protected String requiredLine(String prompt) {
    String value = readLine(prompt);
    if (value.isBlank()) throw new IllegalArgumentException("This field is required.");
    return value;
  }

  /** Reads a long integer from a required prompt. */
  protected long readLong(String prompt) {
    return Long.parseLong(requiredLine(prompt));
  }

  /** Reads an integer from a required prompt. */
  protected int readInt(String prompt) {
    return Integer.parseInt(requiredLine(prompt));
  }

  /**
   * Prompts showing the current value in parentheses.
   * If the user presses Enter, the current value is kept.
   */
  protected String value(String label, String current) {
    String suffix = current == null ? "" : " (current: " + current + ")";
    System.out.print(label + suffix + ": ");
    String entered = input.nextLine().trim();
    return entered.isBlank() && current != null ? current : entered;
  }

  // ── Output helpers ─────────────────────────────────────────────────────────

  protected void printError(String message) {
    System.out.println("Error: " + message);
  }

  protected void printSuccess(String message) {
    System.out.println("✓ " + message);
  }

  // ── String utilities ───────────────────────────────────────────────────────

  protected String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  protected String truncate(String text, int max) {
    return text == null ? "" : text.length() > max ? text.substring(0, max) : text;
  }
}
