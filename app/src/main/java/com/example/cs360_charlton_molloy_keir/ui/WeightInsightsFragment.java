package com.example.cs360_charlton_molloy_keir.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cs360_charlton_molloy_keir.R;
import com.example.cs360_charlton_molloy_keir.databinding.FragmentWeightInsightsBinding;
import com.example.cs360_charlton_molloy_keir.model.WeightAnalyticsSummary;
import com.example.cs360_charlton_molloy_keir.service.WeightService;
import com.example.cs360_charlton_molloy_keir.util.DateInputMaskWatcher;
import com.example.cs360_charlton_molloy_keir.util.DateUtil;
import com.example.cs360_charlton_molloy_keir.util.SessionManager;
import com.example.cs360_charlton_molloy_keir.util.ToastUtil;

public class WeightInsightsFragment extends Fragment {

    private static final long NO_LOGGED_IN_USER = SessionManager.NO_LOGGED_IN_USER;
    private static final String KEY_APPLIED_START_DATE = "applied_start_date";
    private static final String KEY_APPLIED_END_DATE = "applied_end_date";
    private static final String KEY_FILTER_EXPANDED = "filter_expanded";

    private FragmentWeightInsightsBinding binding;
    private WeightService weightService;
    private WeightAnalyticsFormatter analyticsFormatter;
    private long userId;
    private String appliedStartDate;
    private String appliedEndDate;
    private boolean filterExpanded;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentWeightInsightsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        weightService = new WeightService(requireContext());
        analyticsFormatter = new WeightAnalyticsFormatter(requireContext());
        userId = weightService.getLoggedInUserId();

        if (!ensureLoggedInSession()) {
            return;
        }

        configureDateInputs();
        configureClickListeners();
        configureFilterToggle();
        restoreSavedState(savedInstanceState);
        syncFilterInputsToAppliedDates();
        applyFilterExpandedState();
        refreshAnalytics();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && weightService != null && userId != NO_LOGGED_IN_USER) {
            refreshAnalytics();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_APPLIED_START_DATE, appliedStartDate);
        outState.putString(KEY_APPLIED_END_DATE, appliedEndDate);
        outState.putBoolean(KEY_FILTER_EXPANDED, filterExpanded);
    }

    private boolean ensureLoggedInSession() {
        if (userId != NO_LOGGED_IN_USER) {
            return true;
        }

        showToast(R.string.toast_login_first);
        NavHostFragment.findNavController(WeightInsightsFragment.this).popBackStack();
        return false;
    }

    private void restoreSavedState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        appliedStartDate = savedInstanceState.getString(KEY_APPLIED_START_DATE);
        appliedEndDate = savedInstanceState.getString(KEY_APPLIED_END_DATE);
        filterExpanded = savedInstanceState.getBoolean(KEY_FILTER_EXPANDED, false);
    }

    private void configureDateInputs() {
        DateInputMaskWatcher.attach(binding.editFilterStartDate);
        DateInputMaskWatcher.attach(binding.editFilterEndDate);
    }

    private void configureClickListeners() {
        binding.buttonApplyFilter.setOnClickListener(v -> loadAnalytics(
                binding.editFilterStartDate.getText().toString(),
                binding.editFilterEndDate.getText().toString(),
                true
        ));

        binding.buttonClearFilter.setOnClickListener(v -> {
            binding.editFilterStartDate.setText("");
            binding.editFilterEndDate.setText("");
            loadAnalytics("", "", true);
        });
    }

    private void configureFilterToggle() {
        binding.layoutFilterHeader.setOnClickListener(v -> {
            filterExpanded = !filterExpanded;
            applyFilterExpandedState();
        });
    }

    private void applyFilterExpandedState() {
        binding.layoutFilterContent.setVisibility(filterExpanded ? View.VISIBLE : View.GONE);
        binding.iconFilterExpand.setRotation(filterExpanded ? 180f : 0f);
    }

    private void refreshAnalytics() {
        loadAnalytics(appliedStartDate, appliedEndDate, false);
    }

    private void loadAnalytics(String startDateText, String endDateText, boolean updateAppliedFilters) {
        WeightService.AnalyticsResult result = weightService.getWeightAnalytics(
                userId,
                startDateText,
                endDateText
        );

        if (result.getStatus() != WeightService.AnalyticsStatus.SUCCESS) {
            showAnalyticsValidationError(result.getStatus());
            return;
        }

        clearFilterErrors();

        if (updateAppliedFilters) {
            appliedStartDate = normalizeOptionalDate(startDateText);
            appliedEndDate = normalizeOptionalDate(endDateText);
            syncFilterInputsToAppliedDates();
        }

        bindAnalytics(result.getSummary());
    }

    private void bindAnalytics(WeightAnalyticsSummary summary) {
        binding.textAnalyticsRange.setText(analyticsFormatter.formatRange(summary));
        binding.textEntriesAnalyzedValue.setText(
                getString(R.string.history_total_entries_value, summary.getFilteredEntries().size())
        );
        binding.textGoalTargetValue.setText(analyticsFormatter.formatGoalTarget(summary.getGoalWeight()));
        binding.textRollingAverageValue.setText(analyticsFormatter.formatWeightMetric(summary.getRollingAverage()));
        binding.textWeeklyChangeValue.setText(analyticsFormatter.formatSignedWeightMetric(summary.getWeeklyChange()));
        binding.textMonthlyChangeValue.setText(analyticsFormatter.formatSignedWeightMetric(summary.getMonthlyChange()));
        binding.textGoalProgressValue.setText(analyticsFormatter.formatProgressMetric(summary));
        binding.textGoalDateLabel.setText(WeightAnalyticsFormatter.getGoalDateLabelRes(summary));
        binding.textProjectedGoalDateValue.setText(analyticsFormatter.formatProjectionMetric(summary));

        boolean hasEntries = !summary.getFilteredEntries().isEmpty();
        if (hasEntries) {
            binding.textInsightsState.setVisibility(View.GONE);
        } else {
            binding.textInsightsState.setVisibility(View.VISIBLE);
            binding.textInsightsState.setText(
                    summary.getTotalEntryCount() == 0
                            ? R.string.empty_entries_message
                            : R.string.empty_filtered_entries_message
            );
        }
    }

    private void showAnalyticsValidationError(WeightService.AnalyticsStatus status) {
        clearFilterErrors();

        switch (status) {
            case INVALID_START_DATE:
                showFieldError(binding.editFilterStartDate, R.string.toast_filter_start_date_invalid);
                break;

            case INVALID_END_DATE:
                showFieldError(binding.editFilterEndDate, R.string.toast_filter_end_date_invalid);
                break;

            case INVALID_RANGE:
                String errorText = getString(R.string.toast_filter_range_invalid);
                binding.editFilterStartDate.setError(errorText);
                binding.editFilterEndDate.setError(errorText);
                showToast(R.string.toast_filter_range_invalid);
                break;

            case SUCCESS:
            default:
                break;
        }
    }

    private void clearFilterErrors() {
        binding.editFilterStartDate.setError(null);
        binding.editFilterEndDate.setError(null);
    }

    private void syncFilterInputsToAppliedDates() {
        binding.editFilterStartDate.setText(appliedStartDate == null ? "" : appliedStartDate);
        binding.editFilterEndDate.setText(appliedEndDate == null ? "" : appliedEndDate);
    }

    private void showToast(int messageResId) {
        ToastUtil.show(requireContext(), messageResId);
    }

    private void showFieldError(EditText editText, int messageResId) {
        editText.setError(getString(messageResId));
        showToast(messageResId);
    }

    private String normalizeOptionalDate(String dateText) {
        String trimmedText = dateText == null ? "" : dateText.trim();
        if (trimmedText.isEmpty()) {
            return null;
        }

        return DateUtil.normalizeDate(trimmedText);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (weightService != null) {
            weightService.close();
            weightService = null;
        }

        analyticsFormatter = null;
        binding = null;
    }
}
