package com.password;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "passwords")
public class Strongpassword {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "user")
    private String user;

    @Column(name = "password")
    private String password;

    public Strongpassword() {
    }

    public Strongpassword(int id, String user, String password) {
        this.id = id;
        this.user = user;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Id= " + id +
               " , user=" + user +
               ", password=" + password;
    }
}