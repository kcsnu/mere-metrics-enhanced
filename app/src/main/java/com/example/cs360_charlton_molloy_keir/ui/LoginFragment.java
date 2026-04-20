package com.example.cs360_charlton_molloy_keir.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cs360_charlton_molloy_keir.R;
import com.example.cs360_charlton_molloy_keir.databinding.FragmentLoginBinding;
import com.example.cs360_charlton_molloy_keir.service.AuthService;
import com.example.cs360_charlton_molloy_keir.util.ToastUtil;

/** Shows the login screen and forwards auth actions to AuthService */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthService authService;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authService = new AuthService(requireContext());

        binding.buttonLogin.setOnClickListener(v -> {
            binding.editPassword.setError(null);

            String username = binding.editUsername.getText().toString().trim();
            String password = binding.editPassword.getText().toString();

            handleLogInResult(authService.logIn(username, password));
        });

        binding.buttonCreateAccount.setOnClickListener(v -> {
            binding.editPassword.setError(null);

            String username = binding.editUsername.getText().toString().trim();
            String password = binding.editPassword.getText().toString();

            handleCreateAccountResult(authService.createAccount(username, password));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (authService != null) {
            authService = null;
        }

        binding = null;
    }

    private void handleLogInResult(AuthService.AuthResult result) {
        switch (result.getStatus()) {
            case MISSING_CREDENTIALS:
                ToastUtil.show(requireContext(), R.string.toast_enter_username_password);
                break;

            case LOGIN_FAILED:
                // Keep the existing behavior of clearing the password after a failed login
                ToastUtil.show(requireContext(), R.string.toast_login_failed);
                binding.editPassword.setText("");
                break;

            case SUCCESS:
                binding.editPassword.setText("");
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_login_fragment_to_weight_tracker_fragment);
                break;

            default:
                break;
        }
    }

    private void handleCreateAccountResult(AuthService.AuthResult result) {
        switch (result.getStatus()) {
            case MISSING_CREDENTIALS:
                ToastUtil.show(requireContext(), R.string.toast_enter_username_password);
                break;

            case USERNAME_TAKEN:
                binding.editPassword.setError(null);
                ToastUtil.show(requireContext(), R.string.toast_username_taken);
                binding.editPassword.setText("");
                break;

            case WEAK_PASSWORD:
                binding.editPassword.setError(getString(R.string.toast_password_too_short));
                ToastUtil.show(requireContext(), R.string.toast_password_too_short);
                break;

            case CREATE_FAILED:
                binding.editPassword.setError(null);
                ToastUtil.show(requireContext(), R.string.toast_create_account_failed);
                binding.editPassword.setText("");
                break;

            case SUCCESS:
                // Keep navigation in the fragment so the service stays UI-independent
                binding.editPassword.setError(null);
                binding.editPassword.setText("");
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_login_fragment_to_weight_tracker_fragment);
                break;

            default:
                break;
        }
    }
}
