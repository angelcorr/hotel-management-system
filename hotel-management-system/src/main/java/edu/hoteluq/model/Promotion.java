package edu.hoteluq.model;

import edu.hoteluq.validation.DomainValidation;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.security.ManagedEntity;
import edu.hoteluq.security.Action;
import java.util.UUID;

public class Promotion extends ManagedEntity {
  private final UUID id = UUID.randomUUID();
  private String name;
  private java.math.BigDecimal discountPercent;

  public Promotion(String name, double discountPercent) {
    this.name = DomainValidation.text(name, "Promotion name");
    this.discountPercent = java.math.BigDecimal.valueOf(validatePercent(discountPercent));
  }

  public static double validatePercent(double percent) {
    DomainValidation.nonNegative(percent, "Discount percent");
    if (percent > 100) throw new HotelException("Discount cannot exceed 100 percent");
    return percent;
  }

  public UUID getId() { return readProtected(() -> id); }
  public String getName() { return readProtected(() -> name); }
  public double getDiscountPercent() { return readProtected(() -> discountPercent.doubleValue()); }
  public java.math.BigDecimal getDiscountPercentAmount() { return readProtected(() -> discountPercent); }
  public Promotion(String name, java.math.BigDecimal percent) {
    this.name = DomainValidation.text(name, "Promotion name");
    if (percent == null || percent.signum() < 0 || percent.compareTo(new java.math.BigDecimal("100")) > 0) throw new HotelException("Invalid percent");
    discountPercent = percent;
  }
  public void setName(String name) {
    String valid = DomainValidation.text(name, "Promotion name");
    change(Action.NAME, valid, () -> this.name = valid);
  }
  public void updateDetails(String name, double percent) {
    String validName = DomainValidation.text(name, "Promotion name");
    double validPercent = validatePercent(percent);
    change(Action.NAME, validName, () -> { this.name = validName; discountPercent = java.math.BigDecimal.valueOf(validPercent); });
  }
  public void setDiscountPercent(double percent) {
    double valid = validatePercent(percent);
    change(Action.UPDATE, valid, () -> discountPercent = java.math.BigDecimal.valueOf(valid));
  }
  public void updateDetails(String name, java.math.BigDecimal percent) {
    String valid=DomainValidation.text(name,"Promotion name");
    if(percent==null || percent.signum()<0 || percent.compareTo(new java.math.BigDecimal("100"))>0) throw new HotelException("Invalid percent");
    change(Action.NAME,valid,()->{this.name=valid;discountPercent=percent;});
  }
  @Override public String toString() { return name + " (" + discountPercent + "%)"; }
}
