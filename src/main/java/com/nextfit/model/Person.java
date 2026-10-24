package com.nextfit.model;

import java.time.LocalDate;
import java.time.Period;

/**
 * A person registered in the gym. It holds the data that every kind of person
 * shares; each subclass (a member, a trainer) adds its own data and says what
 * kind of person it is.
 */
public abstract class Person {

    private final long id;
    private final String fullName;
    private final String documentNumber;
    private final String phone;
    private final String email;
    private final LocalDate birthDate;

    /**
     * Creates a person.
     *
     * @param id             the identifier, or 0 when it has not been assigned yet
     * @param fullName       the full name
     * @param documentNumber the national identity document number
     * @param phone          the phone number
     * @param email          the email address
     * @param birthDate      the date of birth
     */
    protected Person(long id, String fullName, String documentNumber,
                     String phone, String email, LocalDate birthDate) {
        this.id = id;
        this.fullName = fullName;
        this.documentNumber = documentNumber;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
    }

    /**
     * Returns what kind of person this is, in Spanish, to show it in the interface.
     *
     * @return the kind of person, e.g. "Socio"
     */
    public abstract String getPersonType();

    /**
     * Calculates the age of the person on a given date.
     *
     * @param date the date on which the age is calculated
     * @return the age in complete years
     */
    public int ageOn(LocalDate date) {
        return Period.between(birthDate, date).getYears();
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }
}