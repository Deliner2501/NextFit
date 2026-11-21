package com.nextfit.persistence.jdbc;

import com.nextfit.model.Subscription;
import com.nextfit.persistence.SubscriptionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Stores memberships bought by the members in the {@code subscriptions} table.
 */
public final class JdbcSubscriptionRepository implements SubscriptionRepository {

    private static final String TABLE = "subscriptions";
    private static final String[] DATA = {"member_id", "plan_id", "start_date", "end_date", "cancelled"};
    private static final String SELECT = "SELECT id, member_id, plan_id, start_date, end_date, cancelled FROM subscriptions";

    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcSubscriptionRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<Subscription> findAll() {
        return JdbcSupport.query(connections, SELECT + " ORDER BY id", null, JdbcSubscriptionRepository::toEntity);
    }

    @Override
    public Optional<Subscription> findById(long id) {
        return JdbcSupport.query(connections, SELECT + " WHERE id = ?",
                statement -> statement.setLong(1, id), JdbcSubscriptionRepository::toEntity).stream().findFirst();
    }

    @Override
    public List<Subscription> findByMemberId(long memberId) {
        return JdbcSupport.query(connections, SELECT + " WHERE member_id = ? ORDER BY id",
                statement -> statement.setLong(1, memberId), JdbcSubscriptionRepository::toEntity);
    }

    @Override
    public Subscription save(Subscription value) {
        long id = JdbcSupport.save(connections, TABLE, value.getId(), DATA, statement -> {
            statement.setLong(1, value.getMemberId());
            statement.setLong(2, value.getPlanId());
            statement.setObject(3, value.getStartDate());
            statement.setObject(4, value.getEndDate());
            statement.setInt(5, value.isCancelled() ? 1 : 0);
        });
        return new Subscription(id, value.getMemberId(), value.getPlanId(), value.getStartDate(), value.getEndDate(), value.isCancelled());
    }

    private static Subscription toEntity(ResultSet rows) throws SQLException {
        return new Subscription(
                rows.getLong("id"),
                rows.getLong("member_id"),
                rows.getLong("plan_id"),
                rows.getObject("start_date", LocalDate.class),
                rows.getObject("end_date", LocalDate.class),
                rows.getInt("cancelled") == 1);
    }
}
