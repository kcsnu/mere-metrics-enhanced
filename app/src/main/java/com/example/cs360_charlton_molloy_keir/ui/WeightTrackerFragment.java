package com.example.cs360_charlton_molloy_keir.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cs360_charlton_molloy_keir.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.cs360_charlton_molloy_keir.databinding.DialogEditWeightEntryBinding;
import com.example.cs360_charlton_molloy_keir.databinding.DialogGoalWeightBinding;
import com.example.cs360_charlton_molloy_keir.databinding.FragmentWeightTrackerBinding;
import com.example.cs360_charlton_molloy_keir.model.WeightEntry;
import com.example.cs360_charlton_molloy_keir.service.WeightService;
import com.example.cs360_charlton_molloy_keir.util.DateInputMaskWatcher;
import com.example.cs360_charlton_molloy_keir.util.DateUtil;
import com.example.cs360_charlton_molloy_keir.util.FormatUtil;
import com.example.cs360_charlton_molloy_keir.util.SessionManager;
import com.example.cs360_charlton_molloy_keir.util.ToastUtil;

import java.util.List;

/** History-first screen for daily logging, goal management, and entry review */
public class WeightTrackerFragment extends Fragment {

    private static final long NO_LOGGED_IN_USER = SessionManager.NO_LOGGED_IN_USER;

    private FragmentWeightTrackerBinding binding;
    private WeightEntryAdapter adapter;
    private WeightService weightService;
    private long userId;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentWeightTrackerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        weightService = new WeightService(requireContext());
        userId = weightService.getLoggedInUserId();

        if (!ensureLoggedInSession()) {
            return;
        }

        configureRecyclerView();
        configureClickListeners();
        configureInlineEntryCard();
        refreshScreen();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && weightService != null && userId != NO_LOGGED_IN_USER) {
            refreshScreen();
        }
    }

    private boolean ensureLoggedInSession() {
        if (userId != NO_LOGGED_IN_USER) {
            return true;
        }

        showToast(R.string.toast_login_first);
        NavHostFragment.findNavController(WeightTrackerFragment.this).popBackStack();
        return false;
    }

    private void configureRecyclerView() {
        binding.recyclerWeights.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new WeightEntryAdapter(new WeightEntryAdapter.EntryActionListener() {
            @Override
            public void onEdit(WeightEntry entry) {
                showEditDialog(entry);
            }

            @Override
            public void onDelete(WeightEntry entry) {
                deleteEntry(entry);
            }
        });

        binding.recyclerWeights.setAdapter(adapter);
    }

    private void configureClickListeners() {
        binding.buttonSetGoal.setOnClickListener(v -> showGoalDialog());
        binding.buttonOpenInsights.setOnClickListener(v ->
                NavHostFragment.findNavController(WeightTrackerFragment.this)
                        .navigate(R.id.action_weight_tracker_fragment_to_weight_insights_fragment)
        );
        binding.buttonOpenSms.setOnClickListener(v ->
                NavHostFragment.findNavController(WeightTrackerFragment.this)
                        .navigate(R.id.action_weight_tracker_fragment_to_sms_settings_fragment)
        );
    }

    private void configureInlineEntryCard() {
        binding.editInlineDate.setText(DateUtil.getTodayDate());
        DateInputMaskWatcher.attach(binding.editInlineDate);
        binding.buttonAddEntry.setOnClickListener(v -> saveInlineEntry());
    }

    private void saveInlineEntry() {
        WeightService.AddEntryResult result = weightService.addEntry(
                userId,
                binding.editInlineDate.getText().toString(),
                binding.editInlineWeight.getText().toString()
        );

        binding.editInlineDate.setText(result.getResolvedDate());

        switch (result.getStatus()) {
            case INVALID_DATE:
                showFieldError(binding.editInlineDate, R.string.toast_date_invalid);
                break;

            case EMPTY_WEIGHT:
                clearFieldError(binding.editInlineDate);
                showToast(R.string.toast_enter_weight);
                break;

            case WEIGHT_NOT_NUMBER:
                clearFieldError(binding.editInlineDate);
                showToast(R.string.toast_weight_not_number);
                break;

            case WEIGHT_NON_POSITIVE:
                clearFieldError(binding.editInlineDate);
                showToast(R.string.toast_weight_positive);
                break;

            case DUPLICATE_DATE:
                showFieldError(binding.editInlineDate, R.string.toast_entry_duplicate_date);
                break;

            case SAVE_FAILED:
                clearFieldError(binding.editInlineDate);
                showToast(R.string.toast_entry_save_failed);
                break;

            case SUCCESS:
                clearFieldError(binding.editInlineDate);
                showToast(R.string.toast_entry_saved);
                showGoalNotificationToast(result.getGoalNotificationStatus());
                binding.editInlineWeight.setText("");
                binding.editInlineDate.setText(DateUtil.getTodayDate());
                refreshScreen();
                break;

            default:
                break;
        }
    }

    private void refreshScreen() {
        List<WeightEntry> entries = weightService.getWeightEntries(userId);
        adapter.submitEntries(entries);

        boolean hasEntries = !entries.isEmpty();
        binding.recyclerWeights.setVisibility(hasEntries ? View.VISIBLE : View.GONE);
        binding.textEmptyEntries.setVisibility(hasEntries ? View.GONE : View.VISIBLE);
        binding.textEntriesSubtitle.setText(
                getString(R.string.history_entries_count_value, entries.size())
        );

        if (!hasEntries) {
            binding.textEmptyEntries.setText(R.string.empty_entries_message);
        }

        bindSummary(entries, weightService.getGoalWeight(userId));
    }

    private void bindSummary(List<WeightEntry> entries, Double goalWeight) {
        WeightEntry latestEntry = entries.isEmpty() ? null : entries.get(0);

        binding.textCurrentWeightValue.setText(
                latestEntry == null
                        ? getString(R.string.history_not_available)
                        : getString(R.string.analytics_weight_value, FormatUtil.formatWeight(latestEntry.weight))
        );

        binding.textGoalValue.setText(
                goalWeight == null
                        ? getString(R.string.current_goal_not_set)
                        : getString(R.string.analytics_weight_value, FormatUtil.formatWeight(goalWeight))
        );

        binding.textLastLoggedValue.setText(
                latestEntry == null
                        ? getString(R.string.history_not_available)
                        : latestEntry.date
        );

        binding.textTotalEntriesValue.setText(
                getString(R.string.history_total_entries_value, entries.size())
        );
    }

    private void showGoalDialog() {
        DialogGoalWeightBinding dialogBinding = DialogGoalWeightBinding.inflate(getLayoutInflater());

        Double existingGoal = weightService.getGoalWeight(userId);
        if (existingGoal != null) {
            dialogBinding.editDialogGoalWeight.setText(FormatUtil.formatWeight(existingGoal));
            dialogBinding.editDialogGoalWeight.setSelection(
                    dialogBinding.editDialogGoalWeight.getText().length()
            );
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_set_goal_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> saveGoal(dialog, dialogBinding));
        });
        dialog.show();
    }

    private void saveGoal(AlertDialog dialog, DialogGoalWeightBinding dialogBinding) {
        WeightService.GoalSaveResult result = weightService.saveGoal(
                userId,
                dialogBinding.editDialogGoalWeight.getText().toString()
        );

        switch (result.getStatus()) {
            case EMPTY:
                showToast(R.string.toast_enter_goal);
                break;

            case NOT_NUMBER:
                showToast(R.string.toast_goal_not_number);
                break;

            case NON_POSITIVE:
                showToast(R.string.toast_goal_positive);
                break;

            case SUCCESS:
                showToast(R.string.toast_goal_saved);
                refreshScreen();
                dialog.dismiss();
                break;

            default:
                break;
        }
    }

    private void deleteEntry(WeightEntry entry) {
        boolean deleted = weightService.deleteEntry(userId, entry.id);
        if (!deleted) {
            showToast(R.string.toast_entry_delete_failed);
            return;
        }

        showToast(R.string.toast_entry_deleted);
        refreshScreen();
    }

    private void showEditDialog(WeightEntry entry) {
        DialogEditWeightEntryBinding dialogBinding =
                DialogEditWeightEntryBinding.inflate(getLayoutInflater());

        dialogBinding.editDialogEntryDate.setText(entry.date);
        dialogBinding.editDialogEntryWeight.setText(FormatUtil.formatWeight(entry.weight));
        DateInputMaskWatcher.attach(dialogBinding.editDialogEntryDate);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_edit_entry_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(d -> configureEditDialogSaveAction(dialog, dialogBinding, entry));
        dialog.show();
    }

    private void configureEditDialogSaveAction(
            AlertDialog dialog,
            DialogEditWeightEntryBinding dialogBinding,
            WeightEntry entry
    ) {
        Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        saveButton.setOnClickListener(v -> saveEditedEntry(dialog, dialogBinding, entry));
    }

    private void saveEditedEntry(
            AlertDialog dialog,
            DialogEditWeightEntryBinding dialogBinding,
            WeightEntry entry
    ) {
        WeightService.UpdateEntryResult result = weightService.updateEntry(
                userId,
                entry.id,
                dialogBinding.editDialogEntryDate.getText().toString(),
                dialogBinding.editDialogEntryWeight.getText().toString()
        );

        switch (result.getStatus()) {
            case EMPTY_FIELDS:
                clearFieldError(dialogBinding.editDialogEntryDate);
                showToast(R.string.toast_date_weight_required);
                break;

            case INVALID_DATE:
                showFieldError(dialogBinding.editDialogEntryDate, R.string.toast_date_invalid);
                break;

            case WEIGHT_NOT_NUMBER:
                clearFieldError(dialogBinding.editDialogEntryDate);
                showToast(R.string.toast_weight_not_number);
                break;

            case WEIGHT_NON_POSITIVE:
                clearFieldError(dialogBinding.editDialogEntryDate);
                showToast(R.string.toast_weight_positive);
                break;

            case DUPLICATE_DATE:
                showFieldError(dialogBinding.editDialogEntryDate, R.string.toast_entry_duplicate_date);
                break;

            case UPDATE_FAILED:
                clearFieldError(dialogBinding.editDialogEntryDate);
                showToast(R.string.toast_entry_update_failed);
                break;

            case SUCCESS:
                clearFieldError(dialogBinding.editDialogEntryDate);
                showToast(R.string.toast_entry_updated);
                refreshScreen();
                showGoalNotificationToast(result.getGoalNotificationStatus());
                dialog.dismiss();
                break;

            default:
                break;
        }
    }

    private void showGoalNotificationToast(WeightService.GoalNotificationStatus status) {
        switch (status) {
            case SENT:
                showToast(R.string.toast_sms_sent);
                break;

            case NOT_SENT:
                showToast(R.string.toast_sms_not_sent);
                break;

            case NONE:
            default:
                break;
        }
    }

    private void showToast(int messageResId) {
        ToastUtil.show(requireContext(), messageResId);
    }

    private void showFieldError(EditText editText, int messageResId) {
        editText.setError(getString(messageResId));
        showToast(messageResId);
    }

    private void clearFieldError(EditText editText) {
        editText.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (weightService != null) {
            weightService.close();
            weightService = null;
        }

        if (binding != null) {
            binding.recyclerWeights.setAdapter(null);
        }

        adapter = null;
        binding = null;
    }
}
