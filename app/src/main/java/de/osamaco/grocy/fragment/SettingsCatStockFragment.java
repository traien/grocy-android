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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentSettingsCatStockBinding;
import de.osamaco.grocy.model.BottomSheetEvent;
import de.osamaco.grocy.model.Event;
import de.osamaco.grocy.model.Location;
import de.osamaco.grocy.model.ProductGroup;
import de.osamaco.grocy.model.QuantityUnit;
import de.osamaco.grocy.model.SnackbarMessage;
import de.osamaco.grocy.util.ClickUtil;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.Constants.ARGUMENT;
import de.osamaco.grocy.util.Constants.SETTINGS.STOCK;
import de.osamaco.grocy.viewmodel.SettingsViewModel;

public class SettingsCatStockFragment extends BaseFragment {

  private final static String TAG = SettingsCatStockFragment.class.getSimpleName();

  private FragmentSettingsCatStockBinding binding;
  private MainActivity activity;
  private SettingsViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatStockBinding.inflate(inflater, container, false);
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
    binding.setSharedPrefs(PreferenceManager.getDefaultSharedPreferences(activity));
    binding.setClickUtil(new ClickUtil());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    viewModel.getEventHandler().observe(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.frameMainContainer
        ));
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

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

    viewModel.loadProductPresets();
  }

  @Override
  public void saveInput(String text, Bundle argsBundle) {
    String type = argsBundle.getString(ARGUMENT.TYPE);
    if (type != null && type.equals(STOCK.DUE_SOON_DAYS)) {
      viewModel.setDueSoonDays(text);
    } else if (type != null && type.equals(STOCK.DEFAULT_DUE_DAYS)) {
      viewModel.setDefaultDueDays(text);
    } else if (type != null && type.equals(STOCK.DEFAULT_PURCHASE_AMOUNT)) {
      viewModel.setDefaultPurchaseAmount(text);
    } else if (type != null && type.equals(STOCK.DEFAULT_CONSUME_AMOUNT)) {
      viewModel.setDefaultConsumeAmount(text);
    }
  }

  @Override
  public void selectLocation(Location location) {
    viewModel.setPresetLocation(location);
  }

  @Override
  public void selectProductGroup(ProductGroup productGroup) {
    viewModel.setPresetProductGroup(productGroup);
  }

  @Override
  public void selectQuantityUnit(QuantityUnit quantityUnit) {
    viewModel.setPresetQuantityUnit(quantityUnit);
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
  }
}
