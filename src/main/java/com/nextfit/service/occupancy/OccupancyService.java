package com.nextfit.service.occupancy;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Works out how full the gym is right now, for the TV screen. It only calculates: it reads
 * the attendance through {@link AttendanceService}, never draws and never changes data.
 * The clock is injected, so tests can fix the time.
 */
public class OccupancyService {

    /** People the gym admits when nobody says otherwise. */
    public static final int DEFAULT_CAPACITY = 60;
    /** First hour of the day the gym is open. */
    public static final int OPEN_HOUR = 5;
    /** Hour when the gym closes (the last hour offered is the one before). */
    public static final int CLOSE_HOUR = 22;
    /** Weeks of history used to forecast the day. */
    public static final int HISTORY_WEEKS = 8;

    private static final String UNKNOWN_NAME = "Socio";

    private final AttendanceService attendance;
    private final MemberService members;
    private final int capacity;
    private final Clock clock;
    private final OccupancyForecastService forecasts;

    /**
     * Creates the service with the default capacity.
     *
     * @param attendance the source of entries and exits
     * @param members    the source of member names
     * @param clock      the source of the current time
     */
    public OccupancyService(AttendanceService attendance, MemberService members, Clock clock) {
        this(attendance, members, DEFAULT_CAPACITY, clock);
    }

    /**
     * Creates the service with a given capacity.
     *
     * @param attendance the source of entries and exits
     * @param members    the source of member names
     * @param capacity   maximum people the gym admits, greater than zero
     * @param clock      the source of the current time
     * @throws InvalidDataException when the capacity is not greater than zero
     */
    public OccupancyService(AttendanceService attendance, MemberService members, int capacity, Clock clock) {
        if (capacity <= 0) {
            throw new InvalidDataException("aforo", "debe ser mayor que cero");
        }
        this.attendance = attendance;
        this.members = members;
        this.capacity = capacity;
        this.clock = clock;
        // EXPLICACIÓN: el pronóstico se crea aquí con el mismo aforo y reloj, así la "mejor hora"
        // y las horas del pronóstico salen de una sola regla y nunca se contradicen en la pantalla.
        this.forecasts = new OccupancyForecastService(attendance, capacity, clock);
    }

    /**
     * Takes a picture of the gym at this moment.
     *
     * @return the occupancy, who is inside, the entries of today, the best hour to come
     *         and the forecast of the day
     */
    public OccupancySnapshot snapshot() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<AttendanceRecord> records = attendance.findAll();
        List<AttendanceRecord> inside = insideAt(records, now);
        int percent = (int) Math.min(100, Math.round(inside.size() * 100.0 / capacity));
        OccupancyForecast today = forecasts.forecastFor(now.toLocalDate());
        return new OccupancySnapshot(inside.size(), capacity, percent, OccupancyLevel.of(percent),
                visitorsOf(inside, now), entriesToday(records, now.toLocalDate()),
                quietestHour(today, now), forecastOf(today), now);
    }

    // EXPLICACIÓN: una entrada abierta de un día anterior es una salida olvidada, no una persona
    // dentro. Las de hoy sí cuentan, igual que en el control de acceso, para que las dos
    // pantallas muestren a las mismas personas.
    private static List<AttendanceRecord> insideAt(List<AttendanceRecord> records, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        return records.stream()
                .filter(AttendanceRecord::isOpen)
                .filter(r -> !r.getCheckIn().isAfter(now) && r.getCheckIn().toLocalDate().equals(today))
                .sorted(Comparator.comparing(AttendanceRecord::getCheckIn))
                .toList();
    }

    private List<OccupancySnapshot.Visitor> visitorsOf(List<AttendanceRecord> inside, LocalDateTime now) {
        return inside.stream()
                .map(r -> new OccupancySnapshot.Visitor(firstNameOf(r.getMemberId()),
                        Duration.between(r.getCheckIn(), now).toMinutes()))
                .toList();
    }

    // Solo se muestra el primer nombre: la pantalla del televisor la ve todo el gimnasio.
    private String firstNameOf(long memberId) {
        try {
            return members.findById(memberId).getFullName().trim().split("\\s+")[0];
        } catch (NextFitException e) {
            return UNKNOWN_NAME;
        }
    }

    private static List<Integer> entriesToday(List<AttendanceRecord> records, LocalDate today) {
        int[] counts = new int[24];
        for (AttendanceRecord r : records) {
            if (r.getCheckIn().toLocalDate().equals(today)) {
                counts[r.getCheckIn().getHour()]++;
            }
        }
        return IntStream.of(counts).boxed().toList();
    }

    // La hora más vacía que queda hoy, según el pronóstico del día.
    // Devuelve -1 si no hay historial o si ya no quedan horas abiertas.
    private static int quietestHour(OccupancyForecast today, LocalDateTime now) {
        return today.bestHours(Math.max(OPEN_HOUR, now.getHour() + 1), 1).stream()
                .findFirst()
                .map(HourForecast::hour)
                .orElse(-1);
    }

    // Sin historial no se muestra pronóstico: todo en cero parecería "gimnasio vacío todo el día".
    private static List<HourForecast> forecastOf(OccupancyForecast today) {
        return today.hasHistory() ? today.hours() : List.of();
    }
}