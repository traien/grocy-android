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

package de.osamaco.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import de.osamaco.grocy.R;
import de.osamaco.grocy.fragment.bottomSheetDialog.BaseBottomSheet;
import de.osamaco.grocy.model.BottomSheetEvent;
import de.osamaco.grocy.model.Event;
import de.osamaco.grocy.model.SnackbarMessage;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.Constants.SETTINGS;
import de.osamaco.grocy.util.Constants.SETTINGS_DEFAULT;
import de.osamaco.grocy.util.PrefsUtil;

public class BaseViewModel extends AndroidViewModel {

  private final EventHandler eventHandler;
  private final SharedPreferences sharedPrefs;
  private boolean isSearchVisible;
  private boolean debug;

  public BaseViewModel(@NonNull Application application) {
    super(application);
    eventHandler = new EventHandler();
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    isSearchVisible = false;
  }

  SharedPreferences getSharedPrefs() {
    return sharedPrefs;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  boolean getBeginnerModeEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    );
  }

  boolean isDebuggingEnabled() {
    return debug;
  }

  public boolean isOpenFoodFactsEnabled() {
    return sharedPrefs.getBoolean(
        SETTINGS.BEHAVIOR.FOOD_FACTS,
        SETTINGS_DEFAULT.BEHAVIOR.FOOD_FACTS
    );
  }

  public void showErrorMessage() {
    showMessage(getString(R.string.error_undefined));
  }

  public void showErrorMessage(VolleyError volleyError) {
    // similar method is also in BaseFragment
    if (volleyError != null && volleyError.networkResponse != null) {
      if (volleyError.networkResponse.statusCode == 403) {
        showMessage(getString(R.string.error_permission));
        return;
      }
    }
    showMessage(getString(R.string.error_undefined));
  }

  public void showMessage(@Nullable String message) {
    if (message == null) {
      return;
    }
    showSnackbar(new SnackbarMessage(message));
  }

  public void showMessage(@StringRes int message) {
    showSnackbar(new SnackbarMessage(getString(message)));
  }

  void showSnackbar(@NonNull SnackbarMessage snackbarMessage) {
    eventHandler.setValue(snackbarMessage);
  }

  void showBottomSheet(@NonNull BaseBottomSheet bottomSheet, Bundle bundle) {
    eventHandler.setValue(new BottomSheetEvent(bottomSheet, bundle));
  }

  void showBottomSheet(@NonNull BaseBottomSheet bottomSheet) {
    eventHandler.setValue(new BottomSheetEvent(bottomSheet));
  }

  void navigateUp() {
    eventHandler.setValue(new Event() {
      @Override
      public int getType() {
        return Event.NAVIGATE_UP;
      }
    });
  }

  void sendEvent(int type) {
    eventHandler.setValue(new Event() {
      @Override
      public int getType() {
        return type;
      }
    });
  }

  void sendEvent(@SuppressWarnings("SameParameterValue") int type, Bundle bundle) {
    eventHandler.setValue(new Event() {
      @Override
      public int getType() {
        return type;
      }

      @Override
      public Bundle getBundle() {
        return bundle;
      }
    });
  }

  @NonNull
  public EventHandler getEventHandler() {
    return eventHandler;
  }

  String getString(@StringRes int resId) {
    return getApplication().getString(resId);
  }

  Resources getResources() {
    return getApplication().getResources();
  }

  public boolean isSearchVisible() {
    return isSearchVisible;
  }

  public void setIsSearchVisible(boolean visible) {
    isSearchVisible = visible;
  }
}
