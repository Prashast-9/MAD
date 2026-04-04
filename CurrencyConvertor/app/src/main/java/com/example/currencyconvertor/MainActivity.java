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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_AMOUNT = "state_amount";
    private static final String STATE_FROM = "state_from";
    private static final String STATE_TO = "state_to";
    private static final String STATE_RESULT = "state_result";

    private TextInputLayout amountLayout;
    private TextInputEditText amountInput;
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

        amountLayout = findViewById(R.id.amount_layout);
        amountInput = findViewById(R.id.amount_input);
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

        if (savedInstanceState == null) {
            fromCurrencyInput.setText(getString(R.string.currency_inr), false);
            toCurrencyInput.setText(getString(R.string.currency_usd), false);
            amountInput.setText(getString(R.string.default_amount_sample));
            resultAmount.setText(R.string.placeholder_converted_amount);
        } else {
            restoreFormState(savedInstanceState);
        }

        MaterialButton swapButton = findViewById(R.id.swap_button);
        swapButton.setOnClickListener(v -> {
            swapCurrencies();
            performConversion();
        });

        MaterialButton convertButton = findViewById(R.id.convert_button);
        convertButton.setOnClickListener(v -> performConversion());

        updateCurrencyPairLabel();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_AMOUNT, textOrEmpty(amountInput));
        outState.putString(STATE_FROM, textOrEmpty(fromCurrencyInput));
        outState.putString(STATE_TO, textOrEmpty(toCurrencyInput));
        outState.putString(STATE_RESULT, textOrEmpty(resultAmount));
    }

    private void restoreFormState(Bundle savedInstanceState) {
        String amount = savedInstanceState.getString(STATE_AMOUNT);
        if (amount != null) {
            amountInput.setText(amount);
        }
        String from = savedInstanceState.getString(STATE_FROM);
        if (from != null) {
            fromCurrencyInput.setText(from, false);
        }
        String to = savedInstanceState.getString(STATE_TO);
        if (to != null) {
            toCurrencyInput.setText(to, false);
        }
        String result = savedInstanceState.getString(STATE_RESULT);
        if (result != null) {
            resultAmount.setText(result);
        }
    }

    private static String textOrEmpty(TextView view) {
        CharSequence t = view.getText();
        return t != null ? t.toString() : "";
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

    private void performConversion() {
        amountLayout.setError(null);

        String rawAmount = amountInput.getText() != null ? amountInput.getText().toString().trim() : "";
        if (rawAmount.isEmpty()) {
            amountLayout.setError(getString(R.string.error_amount_empty));
            return;
        }

        final double amount;
        try {
            amount = parseAmount(rawAmount);
        } catch (NumberFormatException e) {
            amountLayout.setError(getString(R.string.error_amount_invalid));
            return;
        }

        String from = fromCurrencyInput.getText().toString().trim();
        String to = toCurrencyInput.getText().toString().trim();
        if (!ExchangeRates.isSupported(from)) {
            Snackbar.make(findViewById(R.id.main), R.string.error_currency_invalid, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (!ExchangeRates.isSupported(to)) {
            Snackbar.make(findViewById(R.id.main), R.string.error_currency_invalid, Snackbar.LENGTH_LONG).show();
            return;
        }

        final double converted;
        try {
            converted = CurrencyConverter.convert(amount, from, to);
        } catch (IllegalArgumentException e) {
            Snackbar.make(findViewById(R.id.main), R.string.error_conversion_failed, Snackbar.LENGTH_LONG).show();
            return;
        }

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        resultAmount.setText(nf.format(converted));
        updateCurrencyPairLabel();
    }

    /**
     * Parses a user-entered amount: trims whitespace, strips grouping commas/spaces, uses {@code '.'} as decimal separator.
     */
    static double parseAmount(String raw) throws NumberFormatException {
        String s = raw.trim().replace(",", "").replace(" ", "");
        if (s.isEmpty()) {
            throw new NumberFormatException("empty");
        }
        return Double.parseDouble(s);
    }
}
