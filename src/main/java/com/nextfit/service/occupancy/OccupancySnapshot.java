package com.nextfit.service.occupancy;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A picture of the gym at one moment: how many people are inside, how full it is, who is
 * inside, the entries of today by hour and the best hour to come. It is immutable and has
 * no logic of drawing: the screen only reads it.
 *
 * @param inside        people inside right now
 * @param capacity      maximum people the gym admits
 * @param percent       occupancy from 0 to 100 (never above 100)
 * @param level         how full the gym is
 * @param visitors      the people inside, the one who entered first at the top
 * @param entriesByHour entries registered today, 24 values, one per hour
 * @param quietestHour  the emptiest hour left today according to the history, or -1 when unknown
 * @param updatedAt     the moment this picture was taken
 */
public record OccupancySnapshot(int inside, int capacity, int percent, OccupancyLevel level,
                                List<Visitor> visitors, List<Integer> entriesByHour,
                                int quietestHour, LocalDateTime updatedAt) {

    /**
     * A person inside the gym. Only the first name is kept, so a public screen does not
     * show the full name of the members.
     *
     * @param firstName     the first name of the member
     * @param minutesInside minutes since the member entered
     */
    public record Visitor(String firstName, long minutesInside) {
    }

    /** Copies the lists so nobody can change the picture after it is taken. */
    public OccupancySnapshot {
        visitors = List.copyOf(visitors);
        entriesByHour = List.copyOf(entriesByHour);
    }
}