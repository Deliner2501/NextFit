package com.nextfit.persistence.jdbc;

import com.nextfit.model.Discount;
import com.nextfit.model.FixedDiscount;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.PercentageDiscount;
import com.nextfit.model.RenewalDiscount;
import com.nextfit.persistence.PaymentRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stores payments in the {@code payments} table. The discount is saved as its
 * type and its value, and rebuilt from them when a payment is read.
 */
public final class JdbcPaymentRepository implements PaymentRepository {

    private static final String TABLE = "payments";
    private static final String[] DATA = {"subscription_id", "member_id", "paid_at", "method", "subtotal",
            "discount_type", "discount_value", "registered_by"};
    private static final String SELECT = "SELECT id, subscription_id, member_id, paid_at, method, subtotal, "
            + "discount_type, discount_value, registered_by FROM payments";

    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcPaymentRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<Payment> findAll() {
        return JdbcSupport.query(connections, SELECT + " ORDER BY id", null, JdbcPaymentRepository::toPayment);
    }

    @Override
    public Optional<Payment> findById(long id) {
        return JdbcSupport.query(connections, SELECT + " WHERE id = ?",
                statement -> statement.setLong(1, id), JdbcPaymentRepository::toPayment).stream().findFirst();
    }

    @Override
    public List<Payment> findByMemberId(long memberId) {
        return JdbcSupport.query(connections, SELECT + " WHERE member_id = ? ORDER BY id",
                statement -> statement.setLong(1, memberId), JdbcPaymentRepository::toPayment);
    }

    @Override
    public Payment save(Payment value) {
        long id = JdbcSupport.save(connections, TABLE, value.getId(), DATA, statement -> {
            statement.setLong(1, value.getSubscriptionId());
            statement.setLong(2, value.getMemberId());
            JdbcSupport.setTimestamp(statement, 3, value.getPaidAt());
            statement.setString(4, value.getMethod().name());
            statement.setLong(5, value.getSubtotal());
            statement.setString(6, value.getDiscount().getType());
            statement.setLong(7, value.getDiscount().getValue());
            statement.setString(8, value.getRegisteredBy());
        });
        return new Payment(id, value.getSubscriptionId(), value.getMemberId(), value.getPaidAt(),
                value.getMethod(), value.getSubtotal(), value.getDiscount(), value.getRegisteredBy());
    }

    private static Payment toPayment(ResultSet rows) throws SQLException {
        return new Payment(
                rows.getLong("id"),
                rows.getLong("subscription_id"),
                rows.getLong("member_id"),
                rows.getObject("paid_at", LocalDateTime.class),
                PaymentMethod.valueOf(rows.getString("method")),
                rows.getLong("subtotal"),
                discountOf(rows.getString("discount_type"), rows.getLong("discount_value")),
                JdbcSupport.emptyIfNull(rows.getString("registered_by")));
    }

    /** Rebuilds the discount from the type and the value stored in the row. */
    private static Discount discountOf(String type, long value) {
        if (type != null && type.startsWith(RenewalDiscount.PREFIX)) {
            return new RenewalDiscount(type.substring(RenewalDiscount.PREFIX.length()), Math.toIntExact(value));
        }
        return switch (type) {
            case NoDiscount.TYPE -> new NoDiscount();
            case PercentageDiscount.TYPE -> new PercentageDiscount((int) value);
            case FixedDiscount.TYPE -> new FixedDiscount(value);
            default -> throw new IllegalArgumentException("Tipo de descuento desconocido: " + type);
        };
    }
}
