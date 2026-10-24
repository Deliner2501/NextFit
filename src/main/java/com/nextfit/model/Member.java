package com.nextfit.model;

import java.time.LocalDate;

/**
 * A person who trains at the gym. Besides the shared personal data it keeps
 * the date the person joined, the path of the profile photo and who to call
 * in an emergency.
 */
public class Member extends Person {

    private final LocalDate joinDate;
    private final String photoPath;
    private final String emergencyContact;
    private final boolean active;

    /**
     * Creates a member.
     *
     * @param id               the identifier, or 0 when it has not been assigned yet
     * @param fullName         the full name
     * @param documentNumber   the national identity document number
     * @param phone            the phone number
     * @param email            the email address
     * @param birthDate        the date of birth
     * @param joinDate         the date the member joined the gym
     * @param photoPath        the path of the profile photo, or an empty text when there is none
     * @param emergencyContact the name and phone of the person to call in an emergency
     * @param active           whether the member is still registered in the gym
     */
    public Member(long id, String fullName, String documentNumber, String phone, String email,
                  LocalDate birthDate, LocalDate joinDate, String photoPath,
                  String emergencyContact, boolean active) {
        super(id, fullName, documentNumber, phone, email, birthDate);
        this.joinDate = joinDate;
        this.photoPath = photoPath;
        this.emergencyContact = emergencyContact;
        this.active = active;
    }

    @Override
    public String getPersonType() {
        return "Socio";
    }

    /**
     * Returns a copy of this member with the given identifier.
     *
     * @param newId the identifier to assign
     * @return the copy
     */
    public Member withId(long newId) {
        return new Member(newId, getFullName(), getDocumentNumber(), getPhone(), getEmail(),
                getBirthDate(), joinDate, photoPath, emergencyContact, active);
    }

    /**
     * Returns a copy of this member marked as active or inactive.
     *
     * @param newActive whether the member is still registered in the gym
     * @return the copy
     */
    public Member withActive(boolean newActive) {
        return new Member(getId(), getFullName(), getDocumentNumber(), getPhone(), getEmail(),
                getBirthDate(), joinDate, photoPath, emergencyContact, newActive);
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public boolean isActive() {
        return active;
    }
}