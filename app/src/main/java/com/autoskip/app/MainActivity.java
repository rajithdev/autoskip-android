package com.autoskip.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/** Home screen: current status, the skip counter, and the pause switch. */
public class MainActivity extends Activity {

    private View hero;
    private TextView chip;
    private TextView status;
    private TextView statusDetail;
    private View turnOn;
    private TextView count;
    private TextView countLabel;
    private View toggleRow;
    private Switch autoSkip;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                refresh();
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hero = findViewById(R.id.hero);
        chip = findViewById(R.id.chip);
        status = findViewById(R.id.status);
        statusDetail = findViewById(R.id.status_detail);
        turnOn = findViewById(R.id.turn_on);
        count = findViewById(R.id.count);
        countLabel = findViewById(R.id.count_label);
        toggleRow = findViewById(R.id.toggle_row);
        autoSkip = findViewById(R.id.auto_skip);

        View.OnClickListener openExplanation = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExplainActivity.class));
            }
        };
        turnOn.setOnClickListener(openExplanation);
        findViewById(R.id.about_row).setOnClickListener(openExplanation);

        toggleRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoSkip.toggle();
            }
        });
        autoSkip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (checked != Prefs.isEnabled(MainActivity.this)) {
                    Prefs.setEnabled(MainActivity.this, checked);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Prefs.get(this).registerOnSharedPreferenceChangeListener(prefsListener);
        refresh();
    }

    @Override
    protected void onPause() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefsListener);
        super.onPause();
    }

    private void refresh() {
        boolean serviceOn = SkipService.isSwitchedOn(this);
        boolean enabled = Prefs.isEnabled(this);
        boolean active = serviceOn && enabled;

        if (active) {
            showStatus(R.string.chip_on, R.string.status_on, R.string.status_on_detail);
        } else if (serviceOn) {
            showStatus(R.string.chip_paused, R.string.status_paused, R.string.status_paused_detail);
            chip.setTextColor(getColor(R.color.state_paused));
        } else {
            showStatus(R.string.chip_off, R.string.status_off, R.string.status_off_detail);
            chip.setTextColor(getColor(R.color.state_off));
        }

        // The hero turns into the brand gradient only while ads are being skipped.
        hero.setBackgroundResource(active ? R.drawable.hero_on : R.drawable.hero_off);
        chip.setBackgroundResource(active ? R.drawable.chip_on : R.drawable.chip_neutral);
        if (active) chip.setTextColor(getColor(R.color.on_grad));
        status.setTextColor(getColor(active ? R.color.on_grad : R.color.text_primary));
        statusDetail.setTextColor(getColor(active ? R.color.on_grad_soft : R.color.text_secondary));
        turnOn.setVisibility(serviceOn ? View.GONE : View.VISIBLE);

        // Pausing means nothing until the service itself is switched on.
        toggleRow.setEnabled(serviceOn);
        toggleRow.setAlpha(serviceOn ? 1f : 0.45f);
        autoSkip.setChecked(enabled);

        int skips = Prefs.skips(this);
        count.setText(String.valueOf(skips));
        countLabel.setText(getResources().getQuantityString(R.plurals.ads_skipped, skips));
    }

    private void showStatus(int chipText, int title, int detail) {
        chip.setText(chipText);
        status.setText(title);
        statusDetail.setText(detail);
    }
}
