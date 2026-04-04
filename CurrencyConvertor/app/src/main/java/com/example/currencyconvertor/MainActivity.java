package com.example.currencyconvertor;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView fromCurrencyInput;
    private AutoCompleteTextView toCurrencyInput;
    private TextView resultAmount;
    private TextView resultCurrencyPair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        fromCurrencyInput = findViewById(R.id.from_currency_input);
        toCurrencyInput = findViewById(R.id.to_currency_input);
        resultAmount = findViewById(R.id.result_amount);
        resultCurrencyPair = findViewById(R.id.result_currency_pair);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.currencies));
        fromCurrencyInput.setAdapter(adapter);
        toCurrencyInput.setAdapter(adapter);
        fromCurrencyInput.setThreshold(0);
        toCurrencyInput.setThreshold(0);

        fromCurrencyInput.setOnClickListener(v -> fromCurrencyInput.showDropDown());
        toCurrencyInput.setOnClickListener(v -> toCurrencyInput.showDropDown());

        fromCurrencyInput.setText(getString(R.string.currency_inr), false);
        toCurrencyInput.setText(getString(R.string.currency_usd), false);

        TextWatcher pairWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCurrencyPairLabel();
            }
        };
        fromCurrencyInput.addTextChangedListener(pairWatcher);
        toCurrencyInput.addTextChangedListener(pairWatcher);

        resultAmount.setText(R.string.placeholder_converted_amount);

        MaterialButton swapButton = findViewById(R.id.swap_button);
        swapButton.setOnClickListener(v -> swapCurrencies());

        MaterialButton convertButton = findViewById(R.id.convert_button);
        convertButton.setOnClickListener(v -> applyDummyConvertPreview());

        TextInputEditText amountInput = findViewById(R.id.amount_input);
        amountInput.setText(getString(R.string.default_amount_sample));

        updateCurrencyPairLabel();
    }

    private void swapCurrencies() {
        CharSequence from = fromCurrencyInput.getText();
        CharSequence to = toCurrencyInput.getText();
        fromCurrencyInput.setText(to, false);
        toCurrencyInput.setText(from, false);
        updateCurrencyPairLabel();
    }

    private void updateCurrencyPairLabel() {
        String from = fromCurrencyInput.getText().toString().trim();
        String to = toCurrencyInput.getText().toString().trim();
        if (from.isEmpty()) {
            from = "—";
        }
        if (to.isEmpty()) {
            to = "—";
        }
        resultCurrencyPair.setText(getString(R.string.currency_pair_format, from, to));
    }

    /**
     * UI-only placeholder: refreshes labels with static sample output (no real conversion).
     */
    private void applyDummyConvertPreview() {
        resultAmount.setText(getString(R.string.dummy_converted_value));
        updateCurrencyPairLabel();
    }
}
