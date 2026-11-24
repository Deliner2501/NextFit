package com.nextfit.persistence.jdbc;

import com.nextfit.model.MembershipPlan;
import com.nextfit.persistence.MembershipPlanRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Stores membership plans in the {@code membership_plans} table.
 */
public final class JdbcMembershipPlanRepository implements MembershipPlanRepository {


    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcMembershipPlanRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<MembershipPlan> findAll() {
        return JdbcSupport.query(connections, "pkg_nf_billing.membership_plans_all", 0, null, JdbcMembershipPlanRepository::toEntity);
    }

    @Override
    public Optional<MembershipPlan> findById(long id) {
        return JdbcSupport.query(connections, "pkg_nf_billing.membership_plans_by_id", 1,
                statement -> statement.setLong(1, id), JdbcMembershipPlanRepository::toEntity).stream().findFirst();
    }

    @Override
    public Optional<MembershipPlan> findByName(String name) {
        return JdbcSupport.query(connections, "pkg_nf_billing.membership_plans_by_name", 1,
                statement -> statement.setString(1, name), JdbcMembershipPlanRepository::toEntity).stream().findFirst();
    }

    @Override
    public MembershipPlan save(MembershipPlan value) {
        long id = JdbcSupport.save(connections, "pkg_nf_billing.membership_plans_save", value.getId(), 5, statement -> {
            statement.setString(1, value.getName());
            statement.setString(2, value.getDescription());
            statement.setInt(3, value.getDurationDays());
            statement.setLong(4, value.getPrice());
            statement.setInt(5, value.isActive() ? 1 : 0);
        });
        return new MembershipPlan(id, value.getName(), value.getDescription(), value.getDurationDays(), value.getPrice(), value.isActive());
    }

    private static MembershipPlan toEntity(ResultSet rows) throws SQLException {
        return new MembershipPlan(
                rows.getLong("id"),
                rows.getString("name"),
                JdbcSupport.emptyIfNull(rows.getString("description")),
                rows.getInt("duration_days"),
                rows.getLong("price"),
                rows.getInt("active") == 1);
    }
}
