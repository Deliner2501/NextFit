package com.nextfit.service;

import com.nextfit.model.MembershipPlan;
import com.nextfit.model.Payment;
import com.nextfit.model.Subscription;
import com.nextfit.persistence.MembershipPlanRepository;
import com.nextfit.persistence.PaymentRepository;
import com.nextfit.persistence.SubscriptionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory repositories of the membership module used by its tests, so no
 * file is touched.
 */
final class BillingFakeRepositories {

    private BillingFakeRepositories() {
    }

    static class FakePlanRepository implements MembershipPlanRepository {
        private final List<MembershipPlan> stored = new ArrayList<>();

        @Override
        public List<MembershipPlan> findAll() {
            return new ArrayList<>(stored);
        }

        @Override
        public Optional<MembershipPlan> findById(long id) {
            return stored.stream().filter(p -> p.getId() == id).findFirst();
        }

        @Override
        public Optional<MembershipPlan> findByName(String name) {
            return stored.stream().filter(p -> p.getName().equalsIgnoreCase(name.trim())).findFirst();
        }

        @Override
        public MembershipPlan save(MembershipPlan plan) {
            MembershipPlan toStore = plan.getId() == 0 ? plan.withId(stored.size() + 1L) : plan;
            stored.removeIf(p -> p.getId() == toStore.getId());
            stored.add(toStore);
            return toStore;
        }
    }

    static class FakeSubscriptionRepository implements SubscriptionRepository {
        private final List<Subscription> stored = new ArrayList<>();

        @Override
        public List<Subscription> findAll() {
            return new ArrayList<>(stored);
        }

        @Override
        public Optional<Subscription> findById(long id) {
            return stored.stream().filter(s -> s.getId() == id).findFirst();
        }

        @Override
        public List<Subscription> findByMemberId(long memberId) {
            return stored.stream().filter(s -> s.getMemberId() == memberId).toList();
        }

        @Override
        public Subscription save(Subscription subscription) {
            Subscription toStore = subscription.getId() == 0
                    ? subscription.withId(stored.size() + 1L) : subscription;
            stored.removeIf(s -> s.getId() == toStore.getId());
            stored.add(toStore);
            return toStore;
        }
    }

    static class FakePaymentRepository implements PaymentRepository {
        private final List<Payment> stored = new ArrayList<>();
        private boolean failOnSave;

        /** Makes the next saves fail, to test what happens when a payment cannot be stored. */
        void failOnSave(boolean fail) {
            this.failOnSave = fail;
        }

        @Override
        public List<Payment> findAll() {
            return new ArrayList<>(stored);
        }

        @Override
        public Optional<Payment> findById(long id) {
            return stored.stream().filter(p -> p.getId() == id).findFirst();
        }

        @Override
        public List<Payment> findByMemberId(long memberId) {
            return stored.stream().filter(p -> p.getMemberId() == memberId).toList();
        }

        @Override
        public Payment save(Payment payment) {
            if (failOnSave) {
                throw new com.nextfit.exceptions.PersistenceException("Disco lleno", null);
            }
            Payment toStore = payment.getId() == 0 ? payment.withId(stored.size() + 1L) : payment;
            stored.removeIf(p -> p.getId() == toStore.getId());
            stored.add(toStore);
            return toStore;
        }
    }
}
