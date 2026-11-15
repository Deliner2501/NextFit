package com.nextfit.model;

/**
 * The password a member uses to enter the gym by himself. Only the hash is kept, never the text.
 *
 * @param memberId     the member it belongs to
 * @param passwordHash the salted hash made by {@code PasswordHasher}
 */
public record MemberCredential(long memberId, String passwordHash) {
}
