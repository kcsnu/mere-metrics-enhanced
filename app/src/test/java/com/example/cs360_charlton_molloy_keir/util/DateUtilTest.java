package com.example.cs360_charlton_molloy_keir.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;

public class DateUtilTest {

    @Test
    public void parseDate_returnsLocalDateForValidInput() {
        LocalDate parsedDate = DateUtil.parseDate(" 03/05/2026 ");

        assertEquals(LocalDate.of(2026, 3, 5), parsedDate);
    }

    @Test
    public void parseDate_returnsNullForInvalidInput() {
        assertNull(DateUtil.parseDate("02/30/2026"));
        assertNull(DateUtil.parseDate("2026-03-05"));
        assertNull(DateUtil.parseDate(null));
    }

    @Test
    public void parseStorageDate_returnsLocalDateForValidInput() {
        LocalDate parsedDate = DateUtil.parseStorageDate(" 2026-03-05 ");

        assertEquals(LocalDate.of(2026, 3, 5), parsedDate);
    }

    @Test
    public void parseStorageDate_returnsNullForInvalidInput() {
        assertNull(DateUtil.parseStorageDate("2026/03/05"));
        assertNull(DateUtil.parseStorageDate("03/05/2026"));
        assertNull(DateUtil.parseStorageDate(null));
    }

    @Test
    public void normalizeDate_returnsCanonicalDisplayPattern() {
        assertEquals("03/05/2026", DateUtil.normalizeDate("03/05/2026"));
        assertEquals("03/05/2026", DateUtil.normalizeDate(" 03/05/2026 "));
    }

    @Test
    public void normalizeDate_returnsNullForInvalidInput() {
        assertNull(DateUtil.normalizeDate("3/5/2026"));
        assertNull(DateUtil.normalizeDate("02/30/2026"));
    }

    @Test
    public void formatDate_returnsDisplayPattern() {
        assertEquals("03/05/2026", DateUtil.formatDate(LocalDate.of(2026, 3, 5)));
    }

    @Test
    public void formatStorageDate_returnsStoragePattern() {
        assertEquals("2026-03-05", DateUtil.formatStorageDate(LocalDate.of(2026, 3, 5)));
    }

    @Test
    public void toStorageDate_convertsDisplayPatternToStoragePattern() {
        assertEquals("2026-03-05", DateUtil.toStorageDate("03/05/2026"));
        assertEquals("2026-03-05", DateUtil.toStorageDate(" 03/05/2026 "));
    }

    @Test
    public void toStorageDate_returnsNullForInvalidDisplayInput() {
        assertNull(DateUtil.toStorageDate("2026-03-05"));
        assertNull(DateUtil.toStorageDate("02/30/2026"));
    }

    @Test
    public void toDisplayDate_convertsStoragePatternToDisplayPattern() {
        assertEquals("03/05/2026", DateUtil.toDisplayDate("2026-03-05"));
        assertEquals("03/05/2026", DateUtil.toDisplayDate(" 2026-03-05 "));
    }

    @Test
    public void toDisplayDate_returnsNullForInvalidStorageInput() {
        assertNull(DateUtil.toDisplayDate("03/05/2026"));
        assertNull(DateUtil.toDisplayDate("2026-02-30"));
    }

    @Test
    public void isValidDate_matchesStrictParsing() {
        assertTrue(DateUtil.isValidDate("12/31/2026"));
        assertFalse(DateUtil.isValidDate("13/31/2026"));
    }

    @Test
    public void toEpochDay_returnsStableNumericValueForFiltering() {
        LocalDate date = LocalDate.of(2026, 3, 5);

        assertEquals(date.toEpochDay(), DateUtil.toEpochDay(date));
    }
}
