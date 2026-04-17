package com.androidapp.attendencecheckqrcode.models.entities;

import java.io.Serializable;

public class User implements Serializable {
    private int id; // ID dạng số (hash từ email)
    private String email;
    private String password;
    private String firstName;
    private String lastName;

    public User(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.id = email.hashCode(); // Tạo ID cố định dựa trên email
    }

    // Constructor đọc từ file
    public User(String line) {
        String[] p = line.split("\\|");
        if (p.length >= 6) {
            this.email = p[0];
            this.password = p[1];
            this.firstName = p[2];
            this.lastName = p[3];
            this.id = this.email.hashCode();
        }
    }

    public String toFileString() {
        return email + "|" + password + "|" + firstName + "|" + lastName;
    }

    // Getters
    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFullName() { return lastName + " " + firstName; }
}