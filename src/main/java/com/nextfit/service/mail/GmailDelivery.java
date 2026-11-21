package com.nextfit.service.mail;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Submits messages to Gmail over TLS using an application password. */
final class GmailDelivery implements MailDelivery {

    private final Session session;
    private final String account;
    private final String password;

    GmailDelivery(String account, String password) {
        this.account = account;
        this.password = password;
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", "smtp.gmail.com");
        properties.setProperty("mail.smtp.port", "465");
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.ssl.enable", "true");
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.connectiontimeout", "10000");
        properties.setProperty("mail.smtp.timeout", "10000");
        properties.setProperty("mail.smtp.writetimeout", "10000");
        session = Session.getInstance(properties);
    }

    @Override
    public void send(String recipient, NextFitEmail email) throws MessagingException {
        Transport.send(buildMessage(recipient, email), account, password);
    }

    /** Creates the message without connecting, allowing the MIME envelope to be tested locally. */
    MimeMessage buildMessage(String recipient, NextFitEmail email) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        InternetAddress sender = new InternetAddress(account, true);
        String charset = StandardCharsets.UTF_8.name();
        try {
            sender.setPersonal("NextFit", charset);
        } catch (UnsupportedEncodingException failure) {
            throw new MessagingException("Could not encode the sender name", failure);
        }
        message.setFrom(sender);
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient, true));
        message.setSubject(email.subject(), charset);
        MimeBodyPart text = new MimeBodyPart();
        text.setText(email.text(), charset);
        MimeBodyPart html = new MimeBodyPart();
        html.setText(email.html(), charset, "html");
        MimeMultipart branded = new MimeMultipart("related");
        branded.addBodyPart(html);
        branded.addBodyPart(logo());
        MimeBodyPart htmlWithLogo = new MimeBodyPart();
        htmlWithLogo.setContent(branded);
        MimeMultipart alternatives = new MimeMultipart("alternative");
        alternatives.addBodyPart(text);
        alternatives.addBodyPart(htmlWithLogo);
        message.setContent(alternatives);
        message.saveChanges();
        return message;
    }

    private static MimeBodyPart logo() throws MessagingException {
        try (var image = GmailDelivery.class.getResourceAsStream("/images/logo/nextfit-logo-128.png")) {
            if (image == null) {
                throw new IOException("NextFit logo resource is missing");
            }
            var logo = new MimeBodyPart();
            logo.setDataHandler(new DataHandler(new ByteArrayDataSource(image.readAllBytes(), "image/png")));
            logo.setContentID("<nextfit-logo>");
            logo.setDisposition(MimeBodyPart.INLINE);
            return logo;
        } catch (IOException failure) {
            throw new MessagingException("Could not load the NextFit logo", failure);
        }
    }
}
