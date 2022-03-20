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

package de.osamaco.grocy.fragment;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import java.util.Locale;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentSettingsCatAppearanceBinding;
import de.osamaco.grocy.fragment.bottomSheetDialog.LanguagesBottomSheet;
import de.osamaco.grocy.model.Language;
import de.osamaco.grocy.util.ClickUtil;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.LocaleUtil;
import de.osamaco.grocy.util.ViewUtil;
import de.osamaco.grocy.viewmodel.SettingsViewModel;

public class SettingsCatAppearanceFragment extends BaseFragment {

  private final static String TAG = SettingsCatAppearanceFragment.class.getSimpleName();

  private FragmentSettingsCatAppearanceBinding binding;
  private MainActivity activity;
  private SettingsViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatAppearanceBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setViewModel(viewModel);
    binding.setClickUtil(new ClickUtil());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    int theme = viewModel.getTheme();
    boolean currentlyDark = isSystemThemeDark();
    Drawable imageLightToDark = ContextCompat.getDrawable(
        requireContext(),
        R.drawable.ic_round_light_to_dark_anim
    );
    Drawable imageDarkToLight = ContextCompat.getDrawable(
        requireContext(),
        R.drawable.ic_round_dark_to_light_anim
    );
    if (theme == SettingsViewModel.THEME_SYSTEM) {
      binding.imageTheme.setImageDrawable(currentlyDark ? imageDarkToLight : imageLightToDark);
    } else if (theme == SettingsViewModel.THEME_LIGHT) {
      binding.imageTheme.setImageDrawable(imageLightToDark);
    } else {  // dark
      binding.imageTheme.setImageDrawable(imageDarkToLight);
    }

    if (activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
      activity.getScrollBehavior().setUpScroll(binding.scroll);
      activity.getScrollBehavior().setHideOnScroll(true);
      activity.updateBottomAppBar(
          Constants.FAB.POSITION.GONE,
          R.menu.menu_empty,
          () -> {
          }
      );
      activity.binding.fabMain.hide();
    }

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);
  }

  public void setTheme(int theme) {
    binding.radioButtonFollowSystem.setChecked(theme == SettingsViewModel.THEME_SYSTEM);
    binding.radioButtonLight.setChecked(theme == SettingsViewModel.THEME_LIGHT);
    binding.radioButtonDark.setChecked(theme == SettingsViewModel.THEME_DARK);

    int currentTheme = viewModel.getTheme();

    boolean currentlyDark = isSystemThemeDark();
    if (currentTheme == SettingsViewModel.THEME_LIGHT && theme == SettingsViewModel.THEME_DARK
        || currentTheme == SettingsViewModel.THEME_DARK && theme == SettingsViewModel.THEME_LIGHT
        || currentTheme == SettingsViewModel.THEME_LIGHT && theme == SettingsViewModel.THEME_SYSTEM && currentlyDark
        || currentTheme == SettingsViewModel.THEME_DARK && theme == SettingsViewModel.THEME_SYSTEM && !currentlyDark
        || currentTheme == SettingsViewModel.THEME_SYSTEM && theme == SettingsViewModel.THEME_LIGHT && currentlyDark
        || currentTheme == SettingsViewModel.THEME_SYSTEM && theme == SettingsViewModel.THEME_DARK && !currentlyDark
    ) {
      ViewUtil.startIcon(binding.imageTheme);
    }
    viewModel.setTheme(theme);
    new Handler().postDelayed(() -> {
      AppCompatDelegate.setDefaultNightMode(theme);
      activity.executeOnStart();
    }, 300);
  }

  @Override
  public void setLanguage(Language language) {
    Locale locale = language != null
        ? LocaleUtil.getLocaleFromCode(language.getCode())
        : LocaleUtil.getNearestSupportedLocale(activity, LocaleUtil.getDeviceLocale());
    binding.textLanguage.setText(
        language != null
            ? locale.getDisplayName()
            : getString(R.string.setting_language_system, locale.getDisplayName())
    );
  }

  public String getLanguage() {
    String code = viewModel.getLanguage();
    Locale locale = code != null
        ? LocaleUtil.getLocaleFromCode(code)
        : LocaleUtil.getNearestSupportedLocale(activity, LocaleUtil.getDeviceLocale());
    return code != null
        ? locale.getDisplayName()
        : getString(R.string.setting_language_system, locale.getDisplayName());
  }

  public void showLanguageSelection() {
    ViewUtil.startIcon(binding.imageLanguage);
    activity.showBottomSheet(new LanguagesBottomSheet());
  }

  private boolean isSystemThemeDark() {
    boolean currentlyDark;
    Object uiModeService = requireContext().getSystemService(Context.UI_MODE_SERVICE);
    if (uiModeService != null) {
      currentlyDark = ((UiModeManager) uiModeService).getNightMode()
          == UiModeManager.MODE_NIGHT_YES;
    } else {
      currentlyDark = (getResources().getConfiguration().uiMode
          & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
    return currentlyDark;
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
  }
}
