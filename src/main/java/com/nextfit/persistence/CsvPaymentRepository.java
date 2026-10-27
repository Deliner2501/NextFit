package com.nextfit.persistence;

import com.nextfit.model.Payment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores payments in a CSV file through {@link CsvStore}.
 */
public class CsvPaymentRepository implements PaymentRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvPaymentRepository(Path file) {
        this.store = new CsvStore(file, PaymentCsvMapper.COLUMNS);
    }

    @Override
    public List<Payment> findAll() {
        List<Payment> payments = new ArrayList<>();
        for (String[] row : store.readAll()) {
            payments.add(PaymentCsvMapper.fromRow(row));
        }
        return payments;
    }

    @Override
    public Optional<Payment> findById(long id) {
        return findAll().stream().filter(p -> p.getId() == id).findFirst();
    }

    @Override
    public List<Payment> findByMemberId(long memberId) {
        return findAll().stream().filter(p -> p.getMemberId() == memberId).toList();
    }

    @Override
    public Payment save(Payment payment) {
        List<Payment> payments = findAll();
        Payment toStore = payment;
        if (payment.getId() == 0) {
            long nextId = payments.stream().mapToLong(Payment::getId).max().orElse(0) + 1;
            toStore = payment.withId(nextId);
            payments.add(toStore);
        } else {
            for (int i = 0; i < payments.size(); i++) {
                if (payments.get(i).getId() == payment.getId()) {
                    payments.set(i, payment);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (Payment p : payments) {
            rows.add(PaymentCsvMapper.toRow(p));
        }
        store.writeAll(rows);
        return toStore;
    }
}
