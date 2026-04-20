package com.example.cs360_charlton_molloy_keir.ui;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cs360_charlton_molloy_keir.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.cs360_charlton_molloy_keir.databinding.FragmentSmsSettingsBinding;
import com.example.cs360_charlton_molloy_keir.service.SmsSettingsService;
import com.example.cs360_charlton_molloy_keir.util.SessionManager;
import com.example.cs360_charlton_molloy_keir.util.SmsUtil;
import com.example.cs360_charlton_molloy_keir.util.ToastUtil;

/** Shows SMS settings and handles Android permission prompts */
public class SmsSettingsFragment extends Fragment {

    private static final long NO_LOGGED_IN_USER = SessionManager.NO_LOGGED_IN_USER;

    private FragmentSmsSettingsBinding binding;
    private SmsSettingsService smsSettingsService;
    private CompoundButton.OnCheckedChangeListener smsToggleListener;
    private long userId;

    // Permission requests stay in the fragment because they are Android UI behavior
    private final ActivityResultLauncher<String> requestSmsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!isAdded() || binding == null || smsSettingsService == null) {
                    return;
                }

                updatePermissionStatus(granted);

                if (!granted) {
                    ToastUtil.show(requireContext(), R.string.sms_permission_denied_toast);
                    updateUiAfterPermissionDenied();
                    return;
                }

                enableSmsIfPhoneValid();
            });

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSmsSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        smsSettingsService = new SmsSettingsService(requireContext());
        userId = smsSettingsService.getLoggedInUserId();

        if (userId == NO_LOGGED_IN_USER) {
            ToastUtil.show(requireContext(), R.string.toast_login_first);
            NavHostFragment.findNavController(SmsSettingsFragment.this).popBackStack();
            return;
        }

        binding.editSmsPhone.setText(smsSettingsService.getSavedPhoneNumber(userId));
        binding.buttonSavePhone.setOnClickListener(v -> savePhoneNumber());

        smsToggleListener = (buttonView, isChecked) -> handleSmsToggle(isChecked);
        binding.switchSmsAlerts.setOnCheckedChangeListener(smsToggleListener);

        refreshStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        if (binding == null || !isAdded() || smsSettingsService == null) {
            return;
        }

        boolean hasPermission = SmsUtil.hasSendSmsPermission(requireContext());
        SmsSettingsService.SmsScreenState state =
                smsSettingsService.getScreenState(userId, hasPermission);

        renderScreenState(state, hasPermission);
    }

    private void updateAlertsStatus(boolean alertsEnabled) {
        if (binding == null) {
            return;
        }

        binding.textSmsAlertsStatusValue.setText(
                alertsEnabled
                        ? R.string.sms_alerts_status_on
                        : R.string.sms_alerts_status_off
        );
    }

    private void updatePermissionStatus(boolean granted) {
        if (binding == null) {
            return;
        }

        binding.textSmsPermissionStatusValue.setText(
                granted
                        ? R.string.sms_permission_status_granted
                        : R.string.sms_permission_status_not_granted
        );
    }

    private void setAlertsSwitchChecked(boolean checked) {
        if (binding == null) {
            return;
        }

        // Temporarily remove the listener so programmatic updates do not retrigger it
        binding.switchSmsAlerts.setOnCheckedChangeListener(null);
        binding.switchSmsAlerts.setChecked(checked);
        binding.switchSmsAlerts.setOnCheckedChangeListener(smsToggleListener);
    }

    private void savePhoneNumber() {
        SmsSettingsService.SavePhoneResult result = smsSettingsService.savePhoneNumber(
                userId,
                binding.editSmsPhone.getText().toString()
        );

        switch (result.getStatus()) {
            case MISSING_PHONE:
                binding.editSmsPhone.setError(getString(R.string.sms_phone_missing));
                ToastUtil.show(requireContext(), R.string.sms_phone_missing);
                break;

            case INVALID_PHONE:
                binding.editSmsPhone.setError(getString(R.string.sms_phone_invalid));
                ToastUtil.show(requireContext(), R.string.sms_phone_invalid);
                break;

            case SUCCESS:
                binding.editSmsPhone.setError(null);
                binding.editSmsPhone.setText(result.getSanitizedPhoneNumber());
                ToastUtil.show(requireContext(), R.string.sms_phone_saved_for_account);
                break;

            default:
                break;
        }
    }

    private void handleSmsToggle(boolean isChecked) {
        if (binding == null || !isAdded() || smsSettingsService == null) {
            return;
        }

        if (!isChecked) {
            smsSettingsService.disableAlerts(userId);
            updateAlertsStatus(false);
            return;
        }

        if (!SmsUtil.hasSendSmsPermission(requireContext())) {
            showPermissionDialog();
            return;
        }

        enableSmsIfPhoneValid();
    }

    private void showPermissionDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sms_permission_dialog_title)
                .setMessage(getString(R.string.sms_permission_required))
                .setPositiveButton(
                        R.string.action_continue,
                        (dialogInterface, which) ->
                                requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                )
                .setNegativeButton(
                        R.string.action_not_now,
                        (dialogInterface, which) -> updateUiAfterPermissionDenied()
                )
                .setOnCancelListener(dialogInterface -> updateUiAfterPermissionDenied())
                .create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void enableSmsIfPhoneValid() {
        boolean hasPermission = SmsUtil.hasSendSmsPermission(requireContext());
        SmsSettingsService.EnableAlertsResult result = smsSettingsService.enableAlertsIfPhoneValid(
                userId,
                binding.editSmsPhone.getText().toString(),
                hasPermission
        );

        updatePermissionStatus(hasPermission);

        switch (result.getStatus()) {
            case MISSING_PHONE:
                ToastUtil.show(requireContext(), R.string.sms_phone_missing);
                setAlertsSwitchChecked(false);
                updateAlertsStatus(false);
                break;

            case INVALID_PHONE:
                binding.editSmsPhone.setError(getString(R.string.sms_phone_invalid));
                ToastUtil.show(requireContext(), R.string.sms_phone_invalid);
                setAlertsSwitchChecked(false);
                updateAlertsStatus(false);
                break;

            case ENABLED:
                binding.editSmsPhone.setError(null);
                binding.editSmsPhone.setText(result.getSanitizedPhoneNumber());
                setAlertsSwitchChecked(true);
                updateAlertsStatus(true);
                break;

            case PERMISSION_MISSING:
            default:
                setAlertsSwitchChecked(false);
                updateAlertsStatus(false);
                break;
        }
    }

    private void renderScreenState(
            SmsSettingsService.SmsScreenState state,
            boolean hasPermission
    ) {
        updatePermissionStatus(hasPermission);
        setAlertsSwitchChecked(state.isSwitchChecked());
        updateAlertsStatus(state.isEffectiveOn());
    }

    private void updateUiAfterPermissionDenied() {
        if (binding == null || smsSettingsService == null) {
            return;
        }

        smsSettingsService.handlePermissionDenied(userId);
        setAlertsSwitchChecked(false);
        updatePermissionStatus(false);
        updateAlertsStatus(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (binding != null) {
            binding.switchSmsAlerts.setOnCheckedChangeListener(null);
        }

        smsToggleListener = null;
        smsSettingsService = null;
        binding = null;
    }
}