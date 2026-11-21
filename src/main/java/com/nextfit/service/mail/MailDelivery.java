package com.nextfit.service.mail;

import jakarta.mail.MessagingException;

/** Delivers an individual notification to one mailbox. */
@FunctionalInterface
interface MailDelivery {

    /** Sends a notification through the configured transport. */
    void send(String recipient, NextFitEmail email) throws MessagingException;
}
