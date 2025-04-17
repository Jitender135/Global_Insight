package com.example.global_insights;

public class User {
    public String fullName;
    public String email;
    public String phone;

    public User() {
        // Needed for Firebase
    }

    public User(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }
}
