package com.example.cs360_charlton_molloy_keir.service;

import android.content.Context;

import com.example.cs360_charlton_molloy_keir.R;
import com.example.cs360_charlton_molloy_keir.data.UserPreferencesRepository;
import com.example.cs360_charlton_molloy_keir.data.WeightRepository;
import com.example.cs360_charlton_molloy_keir.model.NormalizedWeightSeries;
import com.example.cs360_charlton_molloy_keir.model.WeightAnalyticsSummary;
import com.example.cs360_charlton_molloy_keir.model.WeightEntry;
import com.example.cs360_charlton_molloy_keir.util.DateUtil;
import com.example.cs360_charlton_molloy_keir.util.FormatUtil;
import com.example.cs360_charlton_molloy_keir.util.SmsUtil;
import com.example.cs360_charlton_molloy_keir.util.StringUtil;

import java.time.LocalDate;
import java.util.List;

/** Coordinates weight history, analytics, and goal notifications. */
public class WeightService {

    private static final long NO_CACHED_USER = -1L;

    public enum GoalSaveStatus {
        SUCCESS,
        EMPTY,
        NOT_NUMBER,
        NON_POSITIVE
    }

    public enum AddEntryStatus {
        SUCCESS,
        INVALID_DATE,
        EMPTY_WEIGHT,
        WEIGHT_NOT_NUMBER,
        WEIGHT_NON_POSITIVE,
        DUPLICATE_DATE,
        SAVE_FAILED
    }

    public enum UpdateEntryStatus {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_DATE,
        WEIGHT_NOT_NUMBER,
        WEIGHT_NON_POSITIVE,
        DUPLICATE_DATE,
        UPDATE_FAILED
    }

    public enum GoalNotificationStatus {
        NONE,
        SENT,
        NOT_SENT
    }

    public enum AnalyticsStatus {
        SUCCESS,
        INVALID_START_DATE,
        INVALID_END_DATE,
        INVALID_RANGE
    }

    /** Result of saving a goal weight */
    public static final class GoalSaveResult {
        private final GoalSaveStatus status;

        private GoalSaveResult(GoalSaveStatus status) {
            this.status = status;
        }

        public GoalSaveStatus getStatus() {
            return status;
        }
    }

    /** Result of adding a history entry */
    public static final class AddEntryResult {
        private final AddEntryStatus status;
        private final String resolvedDate;
        private final GoalNotificationStatus goalNotificationStatus;

        private AddEntryResult(
                AddEntryStatus status,
                String resolvedDate,
                GoalNotificationStatus goalNotificationStatus
        ) {
            this.status = status;
            this.resolvedDate = resolvedDate;
            this.goalNotificationStatus = goalNotificationStatus;
        }

        public AddEntryStatus getStatus() {
            return status;
        }

        public String getResolvedDate() {
            return resolvedDate;
        }

        public GoalNotificationStatus getGoalNotificationStatus() {
            return goalNotificationStatus;
        }
    }

    /** Result of updating a history entry */
    public static final class UpdateEntryResult {
        private final UpdateEntryStatus status;
        private final GoalNotificationStatus goalNotificationStatus;

        private UpdateEntryResult(
                UpdateEntryStatus status,
                GoalNotificationStatus goalNotificationStatus
        ) {
            this.status = status;
            this.goalNotificationStatus = goalNotificationStatus;
        }

        public UpdateEntryStatus getStatus() {
            return status;
        }

        public GoalNotificationStatus getGoalNotificationStatus() {
            return goalNotificationStatus;
        }
    }

    /** Result of computing weight-history analytics */
    public static final class AnalyticsResult {
        private final AnalyticsStatus status;
        private final WeightAnalyticsSummary summary;

        private AnalyticsResult(AnalyticsStatus status, WeightAnalyticsSummary summary) {
            this.status = status;
            this.summary = summary;
        }

        public AnalyticsStatus getStatus() {
            return status;
        }

        public WeightAnalyticsSummary getSummary() {
            return summary;
        }
    }

    /** Snapshot of goal progress across the full chronological history */
    private static final class GoalProgressSnapshot {
        private final double goalWeight;
        private final double latestWeight;
        private final boolean goalReached;

        private GoalProgressSnapshot(double goalWeight, double latestWeight, boolean goalReached) {
            this.goalWeight = goalWeight;
            this.latestWeight = latestWeight;
            this.goalReached = goalReached;
        }
    }

    private final Context appContext;
    private final WeightRepository weightRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final WeightAnalyticsService weightAnalyticsService;

    private long cachedAnalyticsUserId = NO_CACHED_USER;
    private boolean analyticsSeriesDirty = true;
    private NormalizedWeightSeries cachedAnalyticsSeries;

    public WeightService(Context context) {
        this.appContext = context.getApplicationContext();
        this.weightRepository = new WeightRepository(appContext);
        this.userPreferencesRepository = new UserPreferencesRepository(appContext);
        this.weightAnalyticsService = new WeightAnalyticsService();
    }

    public long getLoggedInUserId() {
        return userPreferencesRepository.getLoggedInUserId();
    }

    public Double getGoalWeight(long userId) {
        return weightRepository.getGoalWeight(userId);
    }

    public GoalSaveResult saveGoal(long userId, String goalText) {
        String normalizedGoalText = StringUtil.safeTrim(goalText);
        if (normalizedGoalText.isEmpty()) {
            return new GoalSaveResult(GoalSaveStatus.EMPTY);
        }

        double goalWeight;
        try {
            goalWeight = Double.parseDouble(normalizedGoalText);
        } catch (NumberFormatException e) {
            return new GoalSaveResult(GoalSaveStatus.NOT_NUMBER);
        }

        if (!isFiniteNumber(goalWeight)) {
            return new GoalSaveResult(GoalSaveStatus.NOT_NUMBER);
        }

        if (goalWeight <= 0) {
            return new GoalSaveResult(GoalSaveStatus.NON_POSITIVE);
        }

        weightRepository.upsertGoalWeight(userId, goalWeight);

        // Saving a different goal weight should allow a future goal-reached notification
        userPreferencesRepository.clearGoalNotified(userId);

        return new GoalSaveResult(GoalSaveStatus.SUCCESS);
    }

    public List<WeightEntry> getWeightEntries(long userId) {
        return weightRepository.getDailyWeightsNewestFirst(userId);
    }

    public AnalyticsResult getWeightAnalytics(long userId, String startDateText, String endDateText) {
        String normalizedStartText = StringUtil.safeTrim(startDateText);
        String normalizedEndText = StringUtil.safeTrim(endDateText);

        LocalDate startDate = null;
        if (!normalizedStartText.isEmpty()) {
            startDate = DateUtil.parseDate(normalizedStartText);
            if (startDate == null) {
                return new AnalyticsResult(AnalyticsStatus.INVALID_START_DATE, null);
            }
        }

        LocalDate endDate = null;
        if (!normalizedEndText.isEmpty()) {
            endDate = DateUtil.parseDate(normalizedEndText);
            if (endDate == null) {
                return new AnalyticsResult(AnalyticsStatus.INVALID_END_DATE, null);
            }
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return new AnalyticsResult(AnalyticsStatus.INVALID_RANGE, null);
        }

        if (startDate != null || endDate != null) {
            return buildBoundedAnalytics(userId, startDate, endDate);
        }

        WeightAnalyticsSummary summary = weightAnalyticsService.buildSummaryFromSeries(
                getAnalyticsSeries(userId),
                weightRepository.getGoalWeight(userId),
                startDate,
                endDate
        );

        return new AnalyticsResult(AnalyticsStatus.SUCCESS, summary);
    }

    private AnalyticsResult buildBoundedAnalytics(
            long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        NormalizedWeightSeries fullSeries = getAnalyticsSeries(userId);
        NormalizedWeightSeries boundedSeries = NormalizedWeightSeries.fromChronologicalEntries(
                weightRepository.getDailyWeightsBetween(userId, startDate, endDate)
        );

        WeightAnalyticsSummary summary = weightAnalyticsService.buildSummaryFromBoundedSeries(
                fullSeries,
                boundedSeries,
                weightRepository.getGoalWeight(userId),
                startDate,
                endDate
        );

        return new AnalyticsResult(AnalyticsStatus.SUCCESS, summary);
    }

    public AddEntryResult addEntry(long userId, String dateText, String weightText) {
        String resolvedDate = StringUtil.safeTrim(dateText);
        if (resolvedDate.isEmpty()) {
            resolvedDate = DateUtil.getTodayDate();
        }

        String normalizedDate = DateUtil.normalizeDate(resolvedDate);
        if (normalizedDate == null) {
            return new AddEntryResult(
                    AddEntryStatus.INVALID_DATE,
                    resolvedDate,
                    GoalNotificationStatus.NONE
            );
        }

        String normalizedWeightText = StringUtil.safeTrim(weightText);
        if (normalizedWeightText.isEmpty()) {
            return new AddEntryResult(
                    AddEntryStatus.EMPTY_WEIGHT,
                    normalizedDate,
                    GoalNotificationStatus.NONE
            );
        }

        double weightValue;
        try {
            weightValue = Double.parseDouble(normalizedWeightText);
        } catch (NumberFormatException e) {
            return new AddEntryResult(
                    AddEntryStatus.WEIGHT_NOT_NUMBER,
                    normalizedDate,
                    GoalNotificationStatus.NONE
            );
        }

        if (!isFiniteNumber(weightValue)) {
            return new AddEntryResult(
                    AddEntryStatus.WEIGHT_NOT_NUMBER,
                    normalizedDate,
                    GoalNotificationStatus.NONE
            );
        }

        if (weightValue <= 0) {
            return new AddEntryResult(
                    AddEntryStatus.WEIGHT_NON_POSITIVE,
                    normalizedDate,
                    GoalNotificationStatus.NONE
            );
        }

        GoalProgressSnapshot previousSnapshot = getGoalProgressSnapshot(userId);

        WeightRepository.AddDailyWeightStatus addStatus =
                weightRepository.addDailyWeight(userId, normalizedDate, weightValue);
        switch (addStatus) {
            case DUPLICATE_DATE:
                return new AddEntryResult(
                        AddEntryStatus.DUPLICATE_DATE,
                        normalizedDate,
                        GoalNotificationStatus.NONE
                );

            case FAILURE:
                return new AddEntryResult(
                        AddEntryStatus.SAVE_FAILED,
                        normalizedDate,
                        GoalNotificationStatus.NONE
                );

            case SUCCESS:
            default:
                break;
        }

        invalidateAnalyticsCache();
        GoalNotificationStatus notificationStatus = maybeSendGoalNotification(
                userId,
                previousSnapshot,
                getGoalProgressSnapshot(userId)
        );

        return new AddEntryResult(AddEntryStatus.SUCCESS, normalizedDate, notificationStatus);
    }

    public UpdateEntryResult updateEntry(
            long userId,
            long entryId,
            String newDate,
            String newWeightText
    ) {
        String normalizedDate = StringUtil.safeTrim(newDate);
        String normalizedWeightText = StringUtil.safeTrim(newWeightText);

        if (normalizedDate.isEmpty() || normalizedWeightText.isEmpty()) {
            return new UpdateEntryResult(
                    UpdateEntryStatus.EMPTY_FIELDS,
                    GoalNotificationStatus.NONE
            );
        }

        String canonicalDate = DateUtil.normalizeDate(normalizedDate);
        if (canonicalDate == null) {
            return new UpdateEntryResult(
                    UpdateEntryStatus.INVALID_DATE,
                    GoalNotificationStatus.NONE
            );
        }

        double weightValue;
        try {
            weightValue = Double.parseDouble(normalizedWeightText);
        } catch (NumberFormatException e) {
            return new UpdateEntryResult(
                    UpdateEntryStatus.WEIGHT_NOT_NUMBER,
                    GoalNotificationStatus.NONE
            );
        }

        if (!isFiniteNumber(weightValue)) {
            return new UpdateEntryResult(
                    UpdateEntryStatus.WEIGHT_NOT_NUMBER,
                    GoalNotificationStatus.NONE
            );
        }

        if (weightValue <= 0) {
            return new UpdateEntryResult(
                    UpdateEntryStatus.WEIGHT_NON_POSITIVE,
                    GoalNotificationStatus.NONE
            );
        }

        GoalProgressSnapshot previousSnapshot = getGoalProgressSnapshot(userId);

        WeightRepository.UpdateDailyWeightResult updateResult = weightRepository.updateDailyWeight(
                userId,
                entryId,
                canonicalDate,
                weightValue
        );
        switch (updateResult.getStatus()) {
            case DUPLICATE_DATE:
                return new UpdateEntryResult(
                        UpdateEntryStatus.DUPLICATE_DATE,
                        GoalNotificationStatus.NONE
                );

            case FAILURE:
                return new UpdateEntryResult(
                        UpdateEntryStatus.UPDATE_FAILED,
                        GoalNotificationStatus.NONE
                );

            case SUCCESS:
            default:
                break;
        }

        invalidateAnalyticsCache();
        GoalNotificationStatus notificationStatus = maybeSendGoalNotification(
                userId,
                previousSnapshot,
                getGoalProgressSnapshot(userId)
        );

        return new UpdateEntryResult(UpdateEntryStatus.SUCCESS, notificationStatus);
    }

    public boolean deleteEntry(long userId, long entryId) {
        boolean deleted = weightRepository.deleteDailyWeight(userId, entryId);
        if (deleted) {
            invalidateAnalyticsCache();
        }
        return deleted;
    }

    public void close() {
        invalidateAnalyticsCache();
        cachedAnalyticsSeries = null;
        cachedAnalyticsUserId = NO_CACHED_USER;
    }

    // Rebuild the normalized series only after the stored history changes.
    private NormalizedWeightSeries getAnalyticsSeries(long userId) {
        if (!analyticsSeriesDirty
                && cachedAnalyticsSeries != null
                && cachedAnalyticsUserId == userId) {
            return cachedAnalyticsSeries;
        }

        cachedAnalyticsSeries = NormalizedWeightSeries.fromChronologicalEntries(
                weightRepository.getDailyWeightsOldestFirst(userId)
        );
        cachedAnalyticsUserId = userId;
        analyticsSeriesDirty = false;
        return cachedAnalyticsSeries;
    }

    private void invalidateAnalyticsCache() {
        analyticsSeriesDirty = true;
    }

    private static boolean isFiniteNumber(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    /** Sends the goal-reached SMS only when the full history changes from not reached to reached */
    private GoalNotificationStatus maybeSendGoalNotification(
            long userId,
            GoalProgressSnapshot previousSnapshot,
            GoalProgressSnapshot currentSnapshot
    ) {
        if (currentSnapshot == null || !currentSnapshot.goalReached) {
            return GoalNotificationStatus.NONE;
        }

        if (previousSnapshot != null && previousSnapshot.goalReached) {
            return GoalNotificationStatus.NONE;
        }

        if (!userPreferencesRepository.isSmsEnabled(userId)) {
            return GoalNotificationStatus.NONE;
        }

        if (userPreferencesRepository.wasGoalAlreadyNotified(userId, currentSnapshot.goalWeight)) {
            return GoalNotificationStatus.NONE;
        }

        String phoneNumber = StringUtil.safeTrim(userPreferencesRepository.getSmsPhoneNumber(userId));
        if (phoneNumber.isEmpty()) {
            return GoalNotificationStatus.NOT_SENT;
        }

        String message = appContext.getString(
                R.string.sms_goal_reached_message,
                FormatUtil.formatWeight(currentSnapshot.latestWeight)
        );

        boolean sent = SmsUtil.trySendSms(appContext, phoneNumber, message);
        if (!sent) {
            return GoalNotificationStatus.NOT_SENT;
        }

        userPreferencesRepository.markGoalNotified(userId, currentSnapshot.goalWeight);
        return GoalNotificationStatus.SENT;
    }

    /** Builds a full-history goal snapshot from the cached normalized series */
    private GoalProgressSnapshot getGoalProgressSnapshot(long userId) {
        Double goalWeight = weightRepository.getGoalWeight(userId);
        if (goalWeight == null) {
            return null;
        }

        NormalizedWeightSeries analyticsSeries = getAnalyticsSeries(userId);
        if (analyticsSeries.isEmpty()) {
            return null;
        }

        double startingWeight = analyticsSeries.get(0).getEntry().weight;
        double latestWeight = analyticsSeries.get(analyticsSeries.size() - 1).getEntry().weight;
        boolean goalReached = WeightAnalyticsService.hasReachedGoal(startingWeight, latestWeight, goalWeight);
        return new GoalProgressSnapshot(goalWeight, latestWeight, goalReached);
    }

}
