package com.example.shared.models;

public class Contact {
    private String name;
    private String countryCode;
    private String phone;

    public Contact(String name, String countryCode, String phone) {
        this.name = name;
        this.countryCode = countryCode;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getPhone() {
        return phone;
    }

    public String getFullPhone() {
        return "+" + countryCode + "-" + phone;
    }
}