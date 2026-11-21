package com.nextfit.service.mail;

import com.nextfit.model.Member;
import com.nextfit.model.Payment;
import com.nextfit.model.Subscription;
import com.nextfit.service.MemberService;
import com.nextfit.service.PaymentService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.ui.Formats;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Processes saved payments and upcoming membership expirations off the JavaFX thread. */
public final class EmailNotifications implements AutoCloseable {

    /** The last recorded delivery state of a payment receipt in the active mode. */
    public enum ReceiptStatus {
        NOT_ATTEMPTED, PENDING, SENT, FAILED, UNAVAILABLE
    }

    private static final int REMINDER_DAYS = 3;
    private static final int SEND_LIMIT_PER_SCAN = 10;

    private final Path directory;
    private final String testRecipient;
    private final MailDelivery delivery;
    private final PaymentService payments;
    private final SubscriptionService subscriptions;
    private final MemberService members;
    private final Clock clock;
    private final LocalDateTime enabledAt;
    private ScheduledExecutorService worker;

    EmailNotifications(Path directory, String testRecipient, MailDelivery delivery, PaymentService payments,
                       SubscriptionService subscriptions, MemberService members, Clock clock) throws IOException {
        this.directory = directory;
        this.testRecipient = testRecipient;
        this.delivery = delivery;
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.members = members;
        this.clock = clock;
        Files.createDirectories(directory);
        Path activation = directory.resolve("enabled-at.txt");
        try {
            Files.writeString(activation, LocalDateTime.now(clock).toString(), StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException existing) {
            // Keep the original cutoff when NextFit restarts.
        }
        enabledAt = LocalDateTime.parse(Files.readString(activation).strip());
    }

    /**
     * Starts email only when local configuration explicitly selects test or live mode.
     *
     * @param data the NextFit data folder
     * @param payments the confirmed payment service
     * @param subscriptions the source of membership dates and renewals
     * @param members the source of member email addresses
     * @param clock the current date and time
     * @return the running service, or null when disabled or misconfigured
     */
    public static EmailNotifications start(Path data, PaymentService payments, SubscriptionService subscriptions,
                                           MemberService members, Clock clock) {
        EmailSettings settings;
        try {
            settings = EmailSettings.load(Path.of(EmailSettings.FILE_NAME), System.getenv());
        } catch (IOException | IllegalArgumentException failure) {
            System.err.println("Correo de NextFit desactivado: no se pudo leer " + EmailSettings.FILE_NAME + ".");
            return null;
        }
        String mode = settings.mode();
        if (!"test".equals(mode) && !"live".equals(mode)) {
            System.out.println("Correo de NextFit desactivado: configura email.mode=test o live en "
                    + EmailSettings.FILE_NAME + ".");
            return null;
        }
        String account = settings.account();
        String password = settings.password().replace(" ", "");
        String testRecipient = "test".equals(mode) ? settings.testRecipient() : null;
        if (!validAddress(account) || password.isBlank()
                || ("test".equals(mode) && !validAddress(testRecipient))) {
            System.err.println("Correo de NextFit desactivado: revisa la cuenta, la contraseña y el destinatario de prueba.");
            return null;
        }
        try {
            EmailNotifications notifications = new EmailNotifications(data.resolve("email").resolve(mode),
                    testRecipient, new GmailDelivery(account, password), payments, subscriptions,
                    members, clock);
            notifications.worker = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "nextfit-email");
                thread.setDaemon(true);
                return thread;
            });
            notifications.worker.scheduleWithFixedDelay(notifications::scanSafely, 0, 2, TimeUnit.MINUTES);
            System.out.println("Correo de NextFit activo en modo " + mode + ". Registro en "
                    + notifications.directory.toAbsolutePath().normalize());
            return notifications;
        } catch (IOException | RuntimeException failure) {
            System.err.println("Correo de NextFit desactivado: no se pudo abrir el registro de envíos.");
            return null;
        }
    }

    private void scanSafely() {
        try {
            scan();
        } catch (RuntimeException | IOException failure) {
            System.err.println("No se pudo completar la revisión de correos de NextFit.");
        }
    }

    /** Finds new receipts and reminders and records each delivery attempt. */
    void scan() throws IOException {
        LocalDate today = LocalDate.now(clock);
        int attempts = 0;
        for (Subscription subscription : subscriptions.findExpiringWithin(REMINDER_DAYS)) {
            if (attempts >= SEND_LIMIT_PER_SCAN || Thread.currentThread().isInterrupted()) {
                return;
            }
            if (subscription.daysLeftOn(today) < 0 || hasRenewed(subscription)) {
                continue;
            }
            Member member = members.findById(subscription.getMemberId());
            if (member.isActive() && notify("expiry-" + subscription.getId() + "-" + subscription.getEndDate(), member,
                    NextFitEmail.create("Tu membresía de NextFit está por vencer", "Tu membresía está por vencer",
                            member.getFullName(), "Te avisamos para que puedas renovar a tiempo.",
                            List.of(new NextFitEmail.Detail("Vence el", Formats.date(subscription.getEndDate()))),
                            "Puedes renovar tu membresía en recepción."))) {
                attempts++;
            }
        }
        for (Payment payment : payments.findAll().reversed()) {
            if (attempts >= SEND_LIMIT_PER_SCAN || Thread.currentThread().isInterrupted()) {
                return;
            }
            if (payment.getPaidAt().isBefore(enabledAt)) {
                continue;
            }
            String key = receiptKey(payment);
            if (Files.exists(directory.resolve(key + ".log"))) {
                continue;
            }
            Member member = members.findById(payment.getMemberId());
            if (notify(key, member, receipt(payment, member))) {
                attempts++;
            }
        }
    }

    private NextFitEmail receipt(Payment payment, Member member) {
        Subscription subscription = subscriptions.findById(payment.getSubscriptionId());
        return NextFitEmail.create("Comprobante de pago NextFit " + payment.getReceiptNumber(),
                    "Comprobante de pago", member.getFullName(),
                    "Registramos tu pago de membresía. Aquí tienes el resumen.",
                    List.of(new NextFitEmail.Detail("Comprobante", payment.getReceiptNumber()),
                            new NextFitEmail.Detail("Fecha", Formats.dateTime(payment.getPaidAt())),
                            new NextFitEmail.Detail("Método de pago", payment.getMethod().getLabel()),
                            new NextFitEmail.Detail("Vigencia", Formats.date(subscription.getStartDate()) + " al "
                                    + Formats.date(subscription.getEndDate())),
                            new NextFitEmail.Detail("Total pagado", Formats.money(payment.getTotal()))),
                    "Guarda este correo como comprobante de tu pago.");
    }

    private static String receiptKey(Payment payment) {
        return "receipt-" + payment.getId() + "-" + payment.getPaidAt().toLocalDate();
    }

    /**
     * Reads the current test or live delivery history for a saved payment.
     *
     * @param payment the payment whose receipt to look up
     * @return the last recorded state
     */
    public ReceiptStatus receiptStatus(Payment payment) {
        try {
            return readReceiptStatus(directory.resolve(receiptKey(payment) + ".log"));
        } catch (IOException failure) {
            return ReceiptStatus.UNAVAILABLE;
        }
    }

    private static ReceiptStatus readReceiptStatus(Path record) throws IOException {
        String last;
        try {
            last = Files.readAllLines(record).stream().reduce((first, next) -> next).orElse("");
        } catch (java.nio.file.NoSuchFileException missing) {
            return ReceiptStatus.NOT_ATTEMPTED;
        }
        if (last.startsWith("FAILED ")) {
            return ReceiptStatus.FAILED;
        }
        return switch (last) {
            case "SENT" -> ReceiptStatus.SENT;
            case "PENDING", "" -> ReceiptStatus.PENDING;
            default -> ReceiptStatus.UNAVAILABLE;
        };
    }

    /**
     * Retries a recorded failure without changing or recreating the payment.
     *
     * @param paymentId the existing payment
     * @return a result to display to the operator
     * @throws IOException if the delivery history cannot be read or updated
     */
    public String retryReceipt(long paymentId) throws IOException {
        Payment payment = payments.findById(paymentId);
        String key = receiptKey(payment);
        Path record = directory.resolve(key + ".log");
        if (!Files.exists(record)) {
            return "Este comprobante no tiene un envío fallido registrado.";
        }
        try (var channel = FileChannel.open(directory.resolve(key + ".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) {
                return "Este comprobante ya se está procesando.";
            }
            if (readReceiptStatus(record) != ReceiptStatus.FAILED) {
                return "Solo se pueden reintentar comprobantes con un envío fallido.";
            }
            Member member = members.findById(payment.getMemberId());
            String recipient = recipient(member);
            if (!validAddress(recipient)) {
                return "El destinatario no tiene un correo válido.";
            }
            NextFitEmail email = receipt(payment, member);
            Files.writeString(record, "PENDING\n", StandardOpenOption.APPEND);
            return deliver(key, record, recipient, email)
                    ? "Gmail aceptó el comprobante."
                    : "El reintento falló. Consulta el diagnóstico en Output.";
        } catch (OverlappingFileLockException busy) {
            return "Este comprobante ya se está procesando.";
        }
    }

    private boolean hasRenewed(Subscription current) {
        return subscriptions.findByMember(current.getMemberId()).stream()
                .anyMatch(other -> !other.isCancelled() && other.getStartDate().equals(current.getEndDate().plusDays(1)));
    }

    private boolean notify(String key, Member member, NextFitEmail email) throws IOException {
        String recipient = recipient(member);
        if (!validAddress(recipient)) {
            return false;
        }
        Path record = directory.resolve(key + ".log");
        try {
            Files.writeString(record, "PENDING\n", StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException existing) {
            return false;
        }
        deliver(key, record, recipient, email);
        return true;
    }

    private String recipient(Member member) {
        return testRecipient == null ? member.getEmail() : testRecipient;
    }

    private boolean deliver(String key, Path record, String recipient, NextFitEmail email) throws IOException {
        String outcome;
        try {
            delivery.send(recipient.strip(), testRecipient == null ? email : email.asTest());
            outcome = "SENT";
        } catch (MessagingException failure) {
            // A network failure can occur after acceptance. Keep the claim to avoid a duplicate.
            Throwable cause = failure.getCause();
            outcome = "FAILED " + failure.getClass().getSimpleName()
                    + (cause == null ? "" : " / " + cause.getClass().getSimpleName());
            System.err.println("Correo de NextFit: " + key + " " + outcome);
        }
        Files.writeString(record, outcome + "\n", StandardOpenOption.APPEND);
        return "SENT".equals(outcome);
    }

    private static boolean validAddress(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            InternetAddress address = new InternetAddress(value.strip(), true);
            address.validate();
            return value.strip().equals(address.getAddress()) && !address.isGroup();
        } catch (AddressException invalid) {
            return false;
        }
    }

    /** Stops the background worker when NextFit closes. */
    @Override
    public void close() {
        if (worker != null) {
            worker.shutdownNow();
        }
    }
}
