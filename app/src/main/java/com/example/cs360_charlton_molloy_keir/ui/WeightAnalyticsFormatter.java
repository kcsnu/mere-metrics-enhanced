package com.example.cs360_charlton_molloy_keir.ui;

import android.content.Context;

import com.example.cs360_charlton_molloy_keir.R;
import com.example.cs360_charlton_molloy_keir.model.WeightAnalyticsSummary;
import com.example.cs360_charlton_molloy_keir.util.DateUtil;
import com.example.cs360_charlton_molloy_keir.util.FormatUtil;

import java.time.LocalDate;
import java.util.Locale;

/** Formats analytics values for the insights screen */
public final class WeightAnalyticsFormatter {

    private final Context appContext;

    public WeightAnalyticsFormatter(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public String formatRange(WeightAnalyticsSummary summary) {
        return appContext.getString(
                R.string.analytics_line_value,
                appContext.getString(R.string.analytics_range_label),
                getRangeValue(summary)
        );
    }

    public String formatGoalTarget(Double goalWeight) {
        if (goalWeight == null) {
            return appContext.getString(R.string.analytics_goal_not_set);
        }

        return appContext.getString(R.string.analytics_weight_value, FormatUtil.formatWeight(goalWeight));
    }

    public String formatWeightMetric(Double value) {
        if (value == null) {
            return appContext.getString(R.string.analytics_not_enough_data);
        }

        return appContext.getString(R.string.analytics_weight_value, FormatUtil.formatWeight(value));
    }

    public String formatSignedWeightMetric(Double value) {
        if (value == null) {
            return appContext.getString(R.string.analytics_not_enough_data);
        }

        return appContext.getString(R.string.analytics_weight_value, formatSignedWeightValue(value));
    }

    public String formatProgressMetric(WeightAnalyticsSummary summary) {
        if (summary.getGoalWeight() == null) {
            return appContext.getString(R.string.analytics_goal_not_set);
        }

        Double percentProgress = summary.getPercentProgressToGoal();
        if (percentProgress == null) {
            return appContext.getString(R.string.analytics_not_enough_data);
        }

        if (shouldDisplayGoalReached(summary)) {
            return appContext.getString(R.string.analytics_goal_reached_display);
        }

        return formatPercentValue(percentProgress);
    }

    public String formatProjectionMetric(WeightAnalyticsSummary summary) {
        if (summary.getGoalWeight() == null) {
            return appContext.getString(R.string.analytics_goal_not_set);
        }

        LocalDate projectedGoalDate = summary.getProjectedGoalDate();
        if (projectedGoalDate == null) {
            return appContext.getString(R.string.analytics_no_projection_available);
        }

        return shouldDisplayGoalReached(summary)
                ? appContext.getString(
                R.string.analytics_goal_reached_on,
                DateUtil.formatDate(projectedGoalDate)
        )
                : DateUtil.formatDate(projectedGoalDate);
    }

    static int getGoalDateLabelRes(WeightAnalyticsSummary summary) {
        return summary != null && summary.isGoalReached()
                ? R.string.analytics_goal_reached_date_label
                : R.string.analytics_projection_label;
    }

    static boolean shouldDisplayGoalReached(WeightAnalyticsSummary summary) {
        if (summary == null) {
            return false;
        }

        if (summary.isGoalReached()) {
            return true;
        }

        Double percentProgress = summary.getPercentProgressToGoal();
        return percentProgress != null && percentProgress >= 100.0;
    }

    static String formatSignedWeightValue(double value) {
        return String.format(Locale.US, "%+.1f", value);
    }

    static String formatPercentValue(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    private String getRangeValue(WeightAnalyticsSummary summary) {
        if (summary.getRequestedStartDate() != null && summary.getRequestedEndDate() != null) {
            return appContext.getString(
                    R.string.analytics_range_between,
                    DateUtil.formatDate(summary.getRequestedStartDate()),
                    DateUtil.formatDate(summary.getRequestedEndDate())
            );
        }

        if (summary.getRequestedStartDate() != null) {
            return appContext.getString(
                    R.string.analytics_range_from,
                    DateUtil.formatDate(summary.getRequestedStartDate())
            );
        }

        if (summary.getRequestedEndDate() != null) {
            return appContext.getString(
                    R.string.analytics_range_until,
                    DateUtil.formatDate(summary.getRequestedEndDate())
            );
        }

        return appContext.getString(R.string.analytics_all_entries);
    }
}
