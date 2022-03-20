/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package de.osamaco.grocy.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import de.osamaco.grocy.R;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.Constants.SETTINGS.APPEARANCE;
import de.osamaco.grocy.util.Constants.SETTINGS_DEFAULT;

public class SplashActivity extends AppCompatActivity {

  public void onCreate(Bundle bundle) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    int theme = sharedPrefs.getInt(APPEARANCE.THEME, SETTINGS_DEFAULT.APPEARANCE.THEME);
    AppCompatDelegate.setDefaultNightMode(theme);

    super.onCreate(bundle);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      startMainActivity();
      return;
    }

    LayerDrawable splashContent = (LayerDrawable) ResourcesCompat.getDrawable(
        getResources(), R.drawable.splash_content, null
    );

    getWindow().setBackgroundDrawable(splashContent);

    boolean speedUpStart = sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.SPEED_UP_START,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.SPEED_UP_START
    );

    try {
      if (speedUpStart) {
        new Handler(Looper.getMainLooper()).postDelayed(
            this::startMainActivity, 50
        );
      } else {
        assert splashContent != null;
        Drawable splashLogo = splashContent.findDrawableByLayerId(R.id.splash_logo);
        AnimatedVectorDrawable logo = (AnimatedVectorDrawable) splashLogo;
        logo.start();
        new Handler(Looper.getMainLooper()).postDelayed(
            this::startMainActivity, 800
        );
      }
    } catch (Exception e) {
      startMainActivity();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  private void startMainActivity() {
    startActivity(new Intent(this, MainActivity.class));
    overridePendingTransition(0, R.anim.splash_fade_out);
    finish();
  }
}
