package com.nextfit.service.mail;

import com.nextfit.model.Member;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.Subscription;
import com.nextfit.service.MemberService;
import com.nextfit.service.PaymentService;
import com.nextfit.service.SubscriptionService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailNotificationsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.now(CLOCK);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final String INBOX = "preview@example.com";
    private static final Member MEMBER = new Member(1, "Andrea Ríos", "10001", "", "member@example.com",
            TODAY.minusYears(25), TODAY.minusYears(1), "", "", true);
    private static final Subscription CURRENT = new Subscription(3, 1, 1, TODAY.minusDays(26), TODAY.plusDays(3), false);

    @TempDir
    Path directory;

    @Test
    void redirectsBothMessagesAndSkipsOldPaymentsAcrossRestarts() throws Exception {
        List<String> messages = new ArrayList<>();
        MailDelivery transport = (to, email) -> messages.add(to + " " + email.subject() + "\n" + email.text());
        List<Payment> payments = List.of(payment(8, NOW.plusSeconds(1)), payment(7, NOW.minusDays(1)));
        service(transport, payments, List.of(CURRENT)).scan();
        service(transport, payments, List.of(CURRENT)).scan();

        assertEquals(2, messages.size());
        assertTrue(messages.stream().allMatch(message -> message.startsWith(INBOX + " [PRUEBA]")));
        assertTrue(messages.get(0).contains("por vencer"));
        assertTrue(messages.get(1).contains("Comprobante de pago"));
        assertTrue(Files.readString(directory.resolve("receipt-8-" + TODAY + ".log")).contains("SENT"));
    }

    @Test
    void skipsRenewedMembershipAndDoesNotRetryAnUncertainDelivery() throws Exception {
        var renewal = new Subscription(4, 1, 1, CURRENT.getEndDate().plusDays(1), TODAY.plusDays(33), false);
        List<String> attempted = new ArrayList<>();
        MailDelivery failed = (to, email) -> {
            attempted.add(email.subject());
            if (attempted.size() == 1) {
                throw new MessagingException("Connection lost");
            }
        };
        List<Payment> payments = List.of(payment(8, NOW));
        service(failed, payments, List.of(CURRENT, renewal)).scan();
        service(failed, payments, List.of(CURRENT, renewal)).scan();

        assertEquals(1, attempted.size());
        assertTrue(attempted.getFirst().contains("Comprobante de pago"));
        assertTrue(Files.readString(directory.resolve("receipt-8-" + TODAY + ".log")).contains("FAILED"));
        var notifications = service(failed, payments, List.of(CURRENT, renewal));
        assertEquals(EmailNotifications.ReceiptStatus.NOT_ATTEMPTED, notifications.receiptStatus(payment(9, NOW)));
        assertEquals(EmailNotifications.ReceiptStatus.FAILED, notifications.receiptStatus(payments.getFirst()));
        assertEquals("Gmail aceptó el comprobante.", notifications.retryReceipt(8));
        assertEquals(EmailNotifications.ReceiptStatus.SENT, notifications.receiptStatus(payments.getFirst()));
        notifications.retryReceipt(8);
        notifications.scan();
        assertEquals(2, attempted.size());
        assertTrue(Files.readString(directory.resolve("receipt-8-" + TODAY + ".log")).endsWith("SENT\n"));
    }

    @Test
    void buildsBrandedMimeAlternativesAndEscapesMemberData() throws Exception {
        NextFitEmail email = NextFitEmail.create("Comprobante de pago", "Pago registrado", "Ana & <Luis>",
                "Registramos tu pago.", List.of(new NextFitEmail.Detail("Total", "$ 30.000")), "Guarda el correo.");
        var message = new GmailDelivery("sender@example.com", "").buildMessage(INBOX, email);
        var parts = (MimeMultipart) message.getContent();

        assertEquals("NextFit", ((InternetAddress) message.getFrom()[0]).getPersonal());
        assertTrue(message.isMimeType("multipart/alternative"));
        assertEquals(2, parts.getCount());
        assertTrue(parts.getBodyPart(0).isMimeType("text/plain"));
        assertTrue(parts.getBodyPart(0).getContent().toString().contains("Ana & <Luis>"));
        assertTrue(parts.getBodyPart(1).isMimeType("multipart/related"));
        var branded = (MimeMultipart) parts.getBodyPart(1).getContent();
        assertTrue(branded.getBodyPart(0).isMimeType("text/html"));
        String html = branded.getBodyPart(0).getContent().toString();
        assertTrue(html.contains("Ana &amp; &lt;Luis&gt;"));
        assertTrue(html.contains("cid:nextfit-logo"));
        assertTrue(branded.getBodyPart(1).isMimeType("image/png"));
        assertEquals("<nextfit-logo>", branded.getBodyPart(1).getHeader("Content-ID")[0]);
    }

    @Test
    void loadsLocalSettingsAndAllowsEnvironmentOverrides() throws Exception {
        Path settingsFile = directory.resolve(EmailSettings.FILE_NAME);
        assertEquals("", EmailSettings.load(settingsFile, Map.of()).mode());
        Files.writeString(settingsFile, """
                email.mode=test
                email.account=sender@example.com
                email.password=fake app password
                email.testRecipient=preview@example.com
                """);
        var settings = EmailSettings.load(settingsFile, Map.of("NEXTFIT_EMAIL_TO", "other@example.com"));
        assertEquals("test", settings.mode());
        assertEquals("sender@example.com", settings.account());
        assertEquals("fake app password", settings.password());
        assertEquals("other@example.com", settings.testRecipient());
        assertEquals("off", EmailSettings.load(settingsFile, Map.of("NEXTFIT_EMAIL_MODE", "off")).mode());
    }

    private EmailNotifications service(MailDelivery delivery, List<Payment> records, List<Subscription> membership)
            throws Exception {
        var members = new MemberService(null, null) {
            @Override public Member findById(long id) { return MEMBER; }
        };
        var subscriptions = new SubscriptionService(null, null, members, CLOCK) {
            @Override public List<Subscription> findExpiringWithin(int days) { return List.of(CURRENT); }
            @Override public List<Subscription> findByMember(long id) { return membership; }
            @Override public Subscription findById(long id) { return CURRENT; }
        };
        var payments = new PaymentService(null, subscriptions, null, null, CLOCK) {
            @Override public List<Payment> findAll() { return records; }
            @Override public Payment findById(long id) {
                return records.stream().filter(payment -> payment.getId() == id).findFirst().orElseThrow();
            }
        };
        return new EmailNotifications(directory, INBOX, delivery, payments, subscriptions, members, CLOCK);
    }

    private static Payment payment(long id, LocalDateTime time) {
        return new Payment(id, CURRENT.getId(), MEMBER.getId(), time, PaymentMethod.CASH,
                30000, new NoDiscount(), "tester");
    }
}
