package com.resala.model;

/** A person or organization that donates. Donors do not log in. */
public class Donor {

    public enum Type {
        INDIVIDUAL,
        ORGANIZATION
    }

    private long id;
    private String name;
    private Type type;
    private String email;
    private String phone;
    private String taxId;

    public Donor() {
    }

    public Donor(long id, String name, Type type, String email, String phone, String taxId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.email = email;
        this.phone = phone;
        this.taxId = taxId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }
}
