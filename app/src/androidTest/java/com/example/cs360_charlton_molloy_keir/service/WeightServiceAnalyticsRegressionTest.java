package com.example.cs360_charlton_molloy_keir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cs360_charlton_molloy_keir.data.room.AppDatabase;
import com.example.cs360_charlton_molloy_keir.data.room.AppDatabaseProvider;
import com.example.cs360_charlton_molloy_keir.model.WeightAnalyticsSummary;
import com.example.cs360_charlton_molloy_keir.model.WeightEntry;
import com.example.cs360_charlton_molloy_keir.support.AnalyticsScenarioFixtures;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.List;

/** Verifies the Room-backed service layer preserves Milestone Three analytics behavior. */
@RunWith(AndroidJUnit4.class)
public class WeightServiceAnalyticsRegressionTest {

    private static final String PREFS_NAME = "cs360_prefs";

    private Context appContext;
    private SharedPreferences preferences;
    private AppDatabase database;
    private WeightService weightService;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        database = AppDatabaseProvider.getInstance(appContext);
        clearDatabase();
        weightService = new WeightService(appContext);
    }

    @After
    public void tearDown() {
        clearDatabase();
        preferences.edit().clear().commit();
        weightService.close();
    }

    @Test
    public void getWeightAnalytics_rebuildsAfterAddUpdateAndDelete() {
        long userId = createUser("analytics_cache_user");
        assertEquals(WeightService.GoalSaveStatus.SUCCESS, weightService.saveGoal(userId, "190").getStatus());

        assertAddEntrySuccess(userId, "03/01/2026", "200.0");
        assertAddEntrySuccess(userId, "03/10/2026", "198.0");

        WeightAnalyticsSummary initialSummary = getAnalyticsSummary(userId, "", "");
        assertEquals(2, initialSummary.getTotalEntryCount());
        assertEquals(198.0, initialSummary.getLatestWeight(), 0.0001);
        assertEquals(20.0, initialSummary.getPercentProgressToGoal(), 0.0001);

        assertAddEntrySuccess(userId, "03/20/2026", "196.0");

        WeightAnalyticsSummary afterAddSummary = getAnalyticsSummary(userId, "", "");
        assertEquals(3, afterAddSummary.getTotalEntryCount());
        assertEquals(196.0, afterAddSummary.getLatestWeight(), 0.0001);
        assertEquals(40.0, afterAddSummary.getPercentProgressToGoal(), 0.0001);
        assertEquals("03/20/2026", afterAddSummary.getFilteredEntries().get(0).date);

        long latestEntryId = weightService.getWeightEntries(userId).get(0).id;
        WeightService.UpdateEntryResult updateResult = weightService.updateEntry(
                userId,
                latestEntryId,
                "03/20/2026",
                "195.0"
        );
        assertEquals(WeightService.UpdateEntryStatus.SUCCESS, updateResult.getStatus());

        WeightAnalyticsSummary afterUpdateSummary = getAnalyticsSummary(userId, "", "");
        assertEquals(3, afterUpdateSummary.getTotalEntryCount());
        assertEquals(195.0, afterUpdateSummary.getLatestWeight(), 0.0001);
        assertEquals(50.0, afterUpdateSummary.getPercentProgressToGoal(), 0.0001);

        assertTrue(weightService.deleteEntry(userId, latestEntryId));

        WeightAnalyticsSummary afterDeleteSummary = getAnalyticsSummary(userId, "", "");
        assertEquals(2, afterDeleteSummary.getTotalEntryCount());
        assertEquals(198.0, afterDeleteSummary.getLatestWeight(), 0.0001);
        assertEquals(20.0, afterDeleteSummary.getPercentProgressToGoal(), 0.0001);
        assertEquals("03/10/2026", afterDeleteSummary.getFilteredEntries().get(0).date);
    }

    @Test
    public void getWeightAnalytics_preservesDisplayDateAndFilterSemanticsThroughRoomRoundTrip() {
        long userId = createUser("analytics_round_trip_user");
        assertEquals(WeightService.GoalSaveStatus.SUCCESS, weightService.saveGoal(userId, "150").getStatus());

        assertAddEntrySuccess(userId, "01/01/2026", "200.0");
        assertAddEntrySuccess(userId, "01/11/2026", "180.0");
        assertAddEntrySuccess(userId, "01/21/2026", "170.0");

        List<WeightEntry> newestFirstEntries = weightService.getWeightEntries(userId);
        assertEquals("01/21/2026", newestFirstEntries.get(0).date);
        assertEquals("01/11/2026", newestFirstEntries.get(1).date);
        assertEquals("01/01/2026", newestFirstEntries.get(2).date);

        assertEquals("2026-01-01", database.dailyWeightDao().getByUserIdOldestFirst(userId).get(0).entryDate);
        assertEquals("2026-01-11", database.dailyWeightDao().getByUserIdOldestFirst(userId).get(1).entryDate);
        assertEquals("2026-01-21", database.dailyWeightDao().getByUserIdOldestFirst(userId).get(2).entryDate);

        WeightAnalyticsSummary filteredSummary = getAnalyticsSummary(
                userId,
                "01/11/2026",
                "01/21/2026"
        );

        assertEquals(3, filteredSummary.getTotalEntryCount());
        assertEquals(2, filteredSummary.getFilteredEntries().size());
        assertEquals("01/21/2026", filteredSummary.getFilteredEntries().get(0).date);
        assertEquals("01/11/2026", filteredSummary.getFilteredEntries().get(1).date);
        assertEquals(LocalDate.of(2026, 1, 11), filteredSummary.getRequestedStartDate());
        assertEquals(LocalDate.of(2026, 1, 21), filteredSummary.getRequestedEndDate());
        assertEquals(LocalDate.of(2026, 1, 11), filteredSummary.getActualStartDate());
        assertEquals(LocalDate.of(2026, 1, 21), filteredSummary.getActualEndDate());
        assertEquals(60.0, filteredSummary.getPercentProgressToGoal(), 0.0001);
        assertFalse(filteredSummary.isGoalReached());
        assertEquals(LocalDate.of(2026, 2, 10), filteredSummary.getProjectedGoalDate());
    }

    @Test
    public void getWeightAnalytics_handlesSparseDateGapsWithoutBreakingRangeSemantics() {
        long userId = createUser("analytics_sparse_gap_user");
        assertEquals(WeightService.GoalSaveStatus.SUCCESS, weightService.saveGoal(userId, "185").getStatus());

        AnalyticsScenarioFixtures.addDisplayWeights(
                weightService,
                userId,
                AnalyticsScenarioFixtures.sparseGapDisplayScenario()
        );

        WeightAnalyticsSummary filteredSummary = getAnalyticsSummary(
                userId,
                "01/10/2026",
                "03/01/2026"
        );

        assertEquals(5, filteredSummary.getTotalEntryCount());
        assertEquals(2, filteredSummary.getFilteredEntries().size());
        assertEquals("02/14/2026", filteredSummary.getFilteredEntries().get(0).date);
        assertEquals("01/24/2026", filteredSummary.getFilteredEntries().get(1).date);
        assertEquals(LocalDate.of(2026, 1, 24), filteredSummary.getActualStartDate());
        assertEquals(LocalDate.of(2026, 2, 14), filteredSummary.getActualEndDate());
        assertEquals(60.0, filteredSummary.getPercentProgressToGoal(), 0.0001);
        assertFalse(filteredSummary.isGoalReached());
    }

    @Test
    public void getWeightAnalytics_keepsGoalReachedThenRegainedSemanticsThroughRoomRoundTrip() {
        long userId = createUser("analytics_goal_regain_user");
        assertEquals(WeightService.GoalSaveStatus.SUCCESS, weightService.saveGoal(userId, "180").getStatus());

        AnalyticsScenarioFixtures.addDisplayWeights(
                weightService,
                userId,
                AnalyticsScenarioFixtures.goalRegainedDisplayScenario()
        );

        WeightAnalyticsSummary fullSummary = getAnalyticsSummary(userId, "", "");
        WeightAnalyticsSummary reachedWindowSummary = getAnalyticsSummary(
                userId,
                "01/01/2026",
                "02/10/2026"
        );

        assertFalse(fullSummary.isGoalReached());
        assertEquals(189.0, fullSummary.getLatestWeight(), 0.0001);
        assertEquals(70.0, fullSummary.getPercentProgressToGoal(), 0.0001);

        assertTrue(reachedWindowSummary.isGoalReached());
        assertEquals(179.5, reachedWindowSummary.getLatestWeight(), 0.0001);
        assertEquals(LocalDate.of(2026, 2, 10), reachedWindowSummary.getProjectedGoalDate());
        assertEquals("02/10/2026", reachedWindowSummary.getFilteredEntries().get(0).date);
    }

    @Test
    public void getWeightAnalytics_keepsFirstGoalDateWhenGoalWasReachedBeforeFilterStart() {
        long userId = createUser("analytics_goal_before_filter_user");
        assertEquals(WeightService.GoalSaveStatus.SUCCESS, weightService.saveGoal(userId, "180").getStatus());

        assertAddEntrySuccess(userId, "01/01/2026", "200.0");
        assertAddEntrySuccess(userId, "01/10/2026", "179.0");
        assertAddEntrySuccess(userId, "01/20/2026", "178.5");
        assertAddEntrySuccess(userId, "01/30/2026", "178.0");

        WeightAnalyticsSummary filteredSummary = getAnalyticsSummary(
                userId,
                "01/15/2026",
                "01/30/2026"
        );

        assertEquals(4, filteredSummary.getTotalEntryCount());
        assertEquals(2, filteredSummary.getFilteredEntries().size());
        assertEquals("01/30/2026", filteredSummary.getFilteredEntries().get(0).date);
        assertEquals("01/20/2026", filteredSummary.getFilteredEntries().get(1).date);
        assertEquals(LocalDate.of(2026, 1, 15), filteredSummary.getRequestedStartDate());
        assertEquals(LocalDate.of(2026, 1, 30), filteredSummary.getRequestedEndDate());
        assertEquals(LocalDate.of(2026, 1, 20), filteredSummary.getActualStartDate());
        assertEquals(LocalDate.of(2026, 1, 30), filteredSummary.getActualEndDate());
        assertTrue(filteredSummary.isGoalReached());
        assertEquals(LocalDate.of(2026, 1, 10), filteredSummary.getProjectedGoalDate());
    }

    @Test
    public void getWeightAnalytics_startOnlyFilterPreservesBoundedEntriesAndFullHistorySemantics() {
        long userId = createUser("analytics_start_only_user");
        assertEquals(WeightService.GoalSaveStatus.SUCCESS, weightService.saveGoal(userId, "180").getStatus());

        assertAddEntrySuccess(userId, "01/01/2026", "200.0");
        assertAddEntrySuccess(userId, "01/10/2026", "195.0");
        assertAddEntrySuccess(userId, "01/20/2026", "190.0");
        assertAddEntrySuccess(userId, "01/30/2026", "185.0");

        WeightAnalyticsSummary filteredSummary = getAnalyticsSummary(userId, "01/10/2026", "");

        assertEquals(4, filteredSummary.getTotalEntryCount());
        assertEquals(3, filteredSummary.getFilteredEntries().size());
        assertEquals("01/30/2026", filteredSummary.getFilteredEntries().get(0).date);
        assertEquals("01/20/2026", filteredSummary.getFilteredEntries().get(1).date);
        assertEquals("01/10/2026", filteredSummary.getFilteredEntries().get(2).date);
        assertEquals(LocalDate.of(2026, 1, 10), filteredSummary.getRequestedStartDate());
        assertEquals(null, filteredSummary.getRequestedEndDate());
        assertEquals(LocalDate.of(2026, 1, 10), filteredSummary.getActualStartDate());
        assertEquals(LocalDate.of(2026, 1, 30), filteredSummary.getActualEndDate());
        assertFalse(filteredSummary.isGoalReached());
    }

    @Test
    public void getWeightAnalytics_endOnlyFilterPreservesBoundedEntriesAndFullHistorySemantics() {
        long userId = createUser("analytics_end_only_user");
        assertEquals(WeightService.GoalSaveStatus.SUCCESS, weightService.saveGoal(userId, "180").getStatus());

        assertAddEntrySuccess(userId, "01/01/2026", "200.0");
        assertAddEntrySuccess(userId, "01/10/2026", "195.0");
        assertAddEntrySuccess(userId, "01/20/2026", "190.0");
        assertAddEntrySuccess(userId, "01/30/2026", "185.0");

        WeightAnalyticsSummary filteredSummary = getAnalyticsSummary(userId, "", "01/20/2026");

        assertEquals(4, filteredSummary.getTotalEntryCount());
        assertEquals(3, filteredSummary.getFilteredEntries().size());
        assertEquals("01/20/2026", filteredSummary.getFilteredEntries().get(0).date);
        assertEquals("01/10/2026", filteredSummary.getFilteredEntries().get(1).date);
        assertEquals("01/01/2026", filteredSummary.getFilteredEntries().get(2).date);
        assertEquals(null, filteredSummary.getRequestedStartDate());
        assertEquals(LocalDate.of(2026, 1, 20), filteredSummary.getRequestedEndDate());
        assertEquals(LocalDate.of(2026, 1, 1), filteredSummary.getActualStartDate());
        assertEquals(LocalDate.of(2026, 1, 20), filteredSummary.getActualEndDate());
        assertFalse(filteredSummary.isGoalReached());
    }

    private WeightAnalyticsSummary getAnalyticsSummary(long userId, String startDate, String endDate) {
        WeightService.AnalyticsResult result = weightService.getWeightAnalytics(userId, startDate, endDate);
        assertEquals(WeightService.AnalyticsStatus.SUCCESS, result.getStatus());
        return result.getSummary();
    }

    private void assertAddEntrySuccess(long userId, String date, String weight) {
        WeightService.AddEntryResult result = weightService.addEntry(userId, date, weight);
        assertEquals(WeightService.AddEntryStatus.SUCCESS, result.getStatus());
        assertEquals(date, result.getResolvedDate());
    }

    private long createUser(String username) {
        return AnalyticsScenarioFixtures.createUser(database, username);
    }

    private void clearDatabase() {
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `UserSettings`");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `GoalWeight`");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `DailyWeights`");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `Users`");
    }
}
