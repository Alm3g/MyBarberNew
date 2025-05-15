package com.example.mybarber;

public class User {

        private String id;
        private String DisplayName;
        private String Email;
        private String Password;
        private Boolean IsBarber;

    public User(String displayName, String email, String password, Boolean isBarber) {
        DisplayName = displayName;
        Email = email;
        Password = password;
        IsBarber = isBarber;
    }
    public User( String email, String password,String id) {
        this.id=id;
        Email = email;
        Password = password;

    }

    public String getDisplayName() {
        return DisplayName;
    }

    public void setDisplayName(String displayName) {
        DisplayName = displayName;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public Boolean getBarber() {
        return IsBarber;
    }

    public void setBarber(Boolean barber) {
        IsBarber = barber;
    }

    @Override
    public String toString() {
        return "User{" +
                "DisplayName='" + DisplayName + '\'' +
                ", Email='" + Email + '\'' +
                ", Password='" + Password + '\'' +
                ", IsBarber=" + IsBarber +
                '}';
    }
}
