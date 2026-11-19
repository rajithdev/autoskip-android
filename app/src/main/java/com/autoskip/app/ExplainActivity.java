package com.autoskip.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

/**
 * Says plainly what AutoSkip does with the Accessibility permission, and
 * prepares the user for Android's warning, before sending them to settings.
 * Opened again later from the main screen it is just the explanation.
 */
public class ExplainActivity extends Activity {

    /** True when we were opened to set the service up, not just to read about it. */
    private boolean settingUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explain);

        settingUp = !SkipService.isSwitchedOn(this);
        ((TextView) findViewById(R.id.title)).setText(
            settingUp ? R.string.explain_title_setup : R.string.explain_title_about);
        int setupVisibility = settingUp ? View.VISIBLE : View.GONE;
        findViewById(R.id.setup_section).setVisibility(setupVisibility);
        findViewById(R.id.actions).setVisibility(setupVisibility);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.continue_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAccessibilitySettings();
            }
        });
        findViewById(R.id.greyed_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGreyedOutHelp();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back from settings with the service now on: setup is finished.
        if (settingUp && SkipService.isSwitchedOn(this)) finish();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        // Asks the Settings app to scroll to and highlight AutoSkip in the list.
        // Phones that don't support the hint simply ignore it.
        String me = new ComponentName(this, SkipService.class).flattenToString();
        Bundle args = new Bundle();
        args.putString(":settings:fragment_args_key", me);
        intent.putExtra(":settings:fragment_args_key", me);
        intent.putExtra(":settings:show_fragment_args", args);
        startActivity(intent);
    }

    private void showGreyedOutHelp() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.greyed_title)
            .setMessage(R.string.greyed_body)
            .setPositiveButton(R.string.open_app_info, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null)));
                }
            })
            .setNegativeButton(R.string.close, null)
            .show();
    }
}
