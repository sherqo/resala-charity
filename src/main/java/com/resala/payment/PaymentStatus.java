package com.resala.payment;

public enum PaymentStatus {
  PENDING {
    @Override public void validate(Payment p) {
      if (p.getReferenceId() != null || p.getPaidAt() != null)
        throw new IllegalStateException("PENDING payment " + p.getId() + " has success fields");
    }
  },
  PAID {
    @Override public void validate(Payment p) {
      if (p.getReferenceType() == null || p.getReferenceId() == null || p.getPaidAt() == null)
        throw new IllegalStateException("PAID payment " + p.getId() + " is missing success fields");
      if (p.getReferenceId() == 0L && !"NONE".equals(p.getReferenceType()))
        throw new IllegalStateException("PAID payment " + p.getId() + " with referenceId 0 must use referenceType NONE");
    }
  },
  FAILED {
    @Override public void validate(Payment p) {
      if (p.getFailureReason() == null)
        throw new IllegalStateException("FAILED payment " + p.getId() + " has no failure reason");
    }
  };

  public abstract void validate(Payment p);

  public boolean isTerminal() { return this != PENDING; }

  public boolean canTransitionTo(PaymentStatus next) {
    return switch (this) {
      case PENDING -> next == PAID || next == FAILED;
      case PAID, FAILED -> false;
    };
  }
}
