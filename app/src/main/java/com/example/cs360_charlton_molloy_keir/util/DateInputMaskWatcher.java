package com.example.cs360_charlton_molloy_keir.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/** Formats date input as MM/dd/yyyy while the user types */
public final class DateInputMaskWatcher implements TextWatcher {

    private final EditText editText;

    // Prevent recursive updates while formatted text is written back
    private boolean isUpdating;

    public DateInputMaskWatcher(EditText editText) {
        this.editText = editText;
    }

    public static void attach(EditText editText) {
        editText.addTextChangedListener(new DateInputMaskWatcher(editText));
    }

    @Override
    public void beforeTextChanged(CharSequence text, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (isUpdating) {
            return;
        }

        isUpdating = true;

        try {
            // Strip non-digits so typing and pasting are handled the same way
            String digits = editable.toString().replaceAll("[^0-9]", "");
            if (digits.length() > 8) {
                digits = digits.substring(0, 8);
            }

            StringBuilder formattedText = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i == 2 || i == 4) {
                    formattedText.append('/');
                }
                formattedText.append(digits.charAt(i));
            }

            editable.replace(0, editable.length(), formattedText.toString());
            editText.setSelection(editable.length());
        } finally {
            isUpdating = false;
        }
    }
}