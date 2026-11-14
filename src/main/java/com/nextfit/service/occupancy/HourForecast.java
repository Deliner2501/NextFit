package com.nextfit.service.occupancy;

/**
 * What is expected for one hour of a day: how many people on average, and what that means for
 * the capacity of the gym.
 *
 * @param hour           the hour of the day, from 0 to 23
 * @param expectedPeople the average number of people inside during that hour
 * @param percent        the expected people as a percentage of the capacity, at most 100
 * @param level          how busy that percentage is
 */
public record HourForecast(int hour, double expectedPeople, int percent, OccupancyLevel level) {
}