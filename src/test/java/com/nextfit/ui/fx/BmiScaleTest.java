package com.nextfit.ui.fx;

import com.nextfit.model.BmiCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BmiScaleTest {

    private static final double DELTA = 1e-9;

    @Test
    void hasFourStretchesThatFillTheBar() {
        assertEquals(4, BmiScale.segments());
        double total = 0;
        for (int i = 0; i < BmiScale.segments(); i++) {
            total += BmiScale.share(i);
        }
        assertEquals(1.0, total, DELTA);
    }

    @Test
    void positionFollowsTheScale() {
        assertEquals(0.0, BmiScale.position(15.0), DELTA);
        assertEquals(0.5, BmiScale.position(27.5), DELTA);
        assertEquals(1.0, BmiScale.position(40.0), DELTA);
    }

    @Test
    void positionStaysInsideTheBar() {
        assertEquals(0.0, BmiScale.position(10.0), DELTA);
        assertEquals(1.0, BmiScale.position(55.0), DELTA);
    }

    @Test
    void stretchesStartWhereTheCategoriesChange() {
        assertEquals(BmiScale.share(0), BmiScale.position(18.5), DELTA);
        assertEquals(BmiScale.share(0) + BmiScale.share(1), BmiScale.position(25.0), DELTA);
        assertEquals(BmiScale.share(0) + BmiScale.share(1) + BmiScale.share(2), BmiScale.position(30.0), DELTA);
        // The limits of the bar are those of the categories the service reports.
        assertEquals(BmiCategory.UNDERWEIGHT, BmiCategory.fromBmi(18.4));
        assertEquals(BmiCategory.NORMAL, BmiCategory.fromBmi(18.5));
        assertEquals(BmiCategory.OVERWEIGHT, BmiCategory.fromBmi(25.0));
        assertEquals(BmiCategory.OBESITY, BmiCategory.fromBmi(30.0));
    }
}
