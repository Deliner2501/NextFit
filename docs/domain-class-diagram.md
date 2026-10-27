# Domain Class Diagram

This diagram contains only the business classes of NextFit (package `model`): what the gym works with, with its inheritance and its relations. Persistence, services and screens are in the [integrated class diagram](integrated-class-diagram.md).

Conventions: getters are omitted; `withX(...)` methods return a modified copy because the classes are immutable; associations between entities are stored as identifiers (for example `Subscription.memberId`), so they are drawn as associations with their multiplicity.

## Hierarchies

```mermaid
classDiagram
    class Person {
        <<abstract>>
    }
    class Member {
        <<concrete>>
    }
    class Trainer {
        <<concrete>>
    }
    class Discount {
        <<abstract>>
    }
    class NoDiscount {
        <<concrete>>
    }
    class PercentageDiscount {
        <<concrete>>
    }
    class FixedDiscount {
        <<concrete>>
    }

    Person <|-- Member
    Person <|-- Trainer
    Discount <|-- NoDiscount
    Discount <|-- PercentageDiscount
    Discount <|-- FixedDiscount
```

## Domain model

```mermaid
classDiagram
    direction TB

    %% ===================== PEOPLE =====================
    class Person {
        <<abstract>>
        -long id
        -String fullName
        -String documentNumber
        -String phone
        -String email
        -LocalDate birthDate
        +ageOn(date LocalDate) int
        +getPersonType()* String
    }
    class Member {
        -LocalDate joinDate
        -String photoPath
        -String emergencyContact
        -boolean active
        +withId(newId long) Member
        +withActive(newActive boolean) Member
        +getPersonType() String
    }
    class Trainer {
        -String specialty
        -String photoPath
        -boolean active
        +withId(newId long) Trainer
        +withActive(newActive boolean) Trainer
        +getPersonType() String
    }
    class User {
        -long id
        -String username
        -String fullName
        -String passwordHash
        -boolean active
        +withId(newId long) User
    }
    class Role {
        <<enumeration>>
        ADMIN
        RECEPTION
        TRAINER
    }

    %% ===================== MEMBERSHIPS AND PAYMENTS =====================
    class MembershipPlan {
        -long id
        -String name
        -String description
        -int durationDays
        -long price
        -boolean active
        +withId(newId long) MembershipPlan
        +withActive(newActive boolean) MembershipPlan
        +endDateFor(start LocalDate) LocalDate
    }
    class Subscription {
        -long id
        -LocalDate startDate
        -LocalDate endDate
        -boolean cancelled
        +cancel() Subscription
        +statusOn(day LocalDate) SubscriptionStatus
        +isActiveOn(day LocalDate) boolean
        +daysLeftOn(day LocalDate) long
        +withId(newId long) Subscription
    }
    class SubscriptionStatus {
        <<enumeration>>
        UPCOMING
        ACTIVE
        EXPIRED
        CANCELLED
    }
    class Payment {
        -long id
        -LocalDateTime paidAt
        -long subtotal
        -String registeredBy
        +getDiscountAmount() long
        +getTotal() long
        +getReceiptNumber() String
        +withId(newId long) Payment
    }
    class PaymentMethod {
        <<enumeration>>
        CASH
        CARD
        TRANSFER
    }
    class Discount {
        <<abstract>>
        +getType()* String
        +getValue()* long
        +amountOff(subtotal long) long
        #calculate(subtotal long)* long
    }
    class NoDiscount
    class PercentageDiscount {
        -int percent
    }
    class FixedDiscount {
        -long amount
    }

    %% ===================== HEALTH AND ATTENDANCE =====================
    class HealthProfile {
        -double heightCm
        -String bloodType
        -String medicalConditions
        -String allergies
    }
    class Measurement {
        -long id
        -LocalDate date
        -double weightKg
        -double waistCm
        -double chestCm
        -double hipsCm
        +withId(newId long) Measurement
    }
    class BmiCategory {
        <<enumeration>>
        UNDERWEIGHT
        NORMAL
        OVERWEIGHT
        OBESITY
        +fromBmi(bmi double)$ BmiCategory
    }
    class AttendanceRecord {
        -long id
        -LocalDateTime checkIn
        -LocalDateTime checkOut
        +isOpen() boolean
        +withCheckOut(exitTime LocalDateTime) AttendanceRecord
        +withId(newId long) AttendanceRecord
    }

    %% ===================== TRAINING =====================
    class Exercise {
        -long id
        -String name
        -String description
        -String imagePath
        +withId(newId long) Exercise
    }
    class MuscleGroup {
        <<enumeration>>
        CHEST
        BACK
        SHOULDERS
        ARMS
        CORE
        LEGS
        GLUTES
        FULL_BODY
        CARDIO
    }
    class Difficulty {
        <<enumeration>>
        BEGINNER
        INTERMEDIATE
        ADVANCED
    }
    class Routine {
        -long id
        -String name
        -String objective
        -LocalDate createdAt
        +withId(newId long) Routine
    }
    class RoutineDay {
        -String name
        +getTotalSets() int
    }
    class RoutineExercise {
        -int sets
        -int repetitions
        -int restSeconds
    }
    class RoutineAssignment {
        -long id
        -LocalDate assignedAt
        +withStatus(newStatus AssignmentStatus) RoutineAssignment
        +withId(newId long) RoutineAssignment
    }
    class AssignmentStatus {
        <<enumeration>>
        ACTIVE
        COMPLETED
        CANCELLED
    }
    class TrainingSession {
        -long id
        -String dayName
        -LocalDate date
        -int durationMinutes
        -String notes
        +withId(newId long) TrainingSession
    }

    %% ===================== INHERITANCE =====================
    Person <|-- Member
    Person <|-- Trainer
    Discount <|-- NoDiscount
    Discount <|-- PercentageDiscount
    Discount <|-- FixedDiscount

    %% ===================== COMPOSITION =====================
    Routine "1" *-- "1..*" RoutineDay : has days
    RoutineDay "1" *-- "1..*" RoutineExercise : has lines
    Payment "1" *-- "1" Discount : applies

    %% ===================== ASSOCIATIONS =====================
    Member "1" -- "0..*" Subscription : buys
    MembershipPlan "1" -- "0..*" Subscription : defines
    Subscription "1" -- "1" Payment : is paid by
    Member "1" -- "0..*" Payment : pays
    Member "1" -- "0..1" HealthProfile : has
    Member "1" -- "0..*" Measurement : is measured
    Member "1" -- "0..*" AttendanceRecord : attends
    Trainer "1" -- "0..*" Routine : creates
    RoutineExercise "0..*" --> "1" Exercise : refers to
    Member "1" -- "0..*" RoutineAssignment : follows
    Routine "1" -- "0..*" RoutineAssignment : is assigned in
    RoutineAssignment "1" -- "0..*" TrainingSession : is logged in

    %% ===================== ENUMERATIONS =====================
    User --> Role
    Payment --> PaymentMethod
    Subscription ..> SubscriptionStatus : computes
    Exercise --> MuscleGroup
    Exercise --> Difficulty
    RoutineAssignment --> AssignmentStatus
    Measurement ..> BmiCategory : classified as
```
