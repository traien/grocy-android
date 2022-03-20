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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentMasterProductCatDueDateBinding;
import de.osamaco.grocy.fragment.bottomSheetDialog.DateBottomSheet;
import de.osamaco.grocy.fragment.bottomSheetDialog.InputBottomSheet;
import de.osamaco.grocy.helper.InfoFullscreenHelper;
import de.osamaco.grocy.model.BottomSheetEvent;
import de.osamaco.grocy.model.Event;
import de.osamaco.grocy.model.FormDataMasterProductCatDueDate;
import de.osamaco.grocy.model.SnackbarMessage;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.Constants.ACTION;
import de.osamaco.grocy.util.Constants.ARGUMENT;
import de.osamaco.grocy.viewmodel.MasterProductCatDueDateViewModel;

public class MasterProductCatDueDateFragment extends BaseFragment {

  private final static String TAG = MasterProductCatDueDateFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentMasterProductCatDueDateBinding binding;
  private MasterProductCatDueDateViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterProductCatDueDateBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    MasterProductFragmentArgs args = MasterProductFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MasterProductCatDueDateViewModel
        .MasterProductCatDueDateViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductCatDueDateViewModel.class);
    binding.setActivity(activity);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        SnackbarMessage message = (SnackbarMessage) event;
        Snackbar snack = message.getSnackbar(activity, activity.binding.frameMainContainer);
        activity.showSnackbar(snack);
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navigateUp();
      } else if (event.getType() == Event.SET_SHOPPING_LIST_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
        setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    if (savedInstanceState == null) {
      viewModel.fillData();
    }

    updateUI(savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.END,
        viewModel.isActionEdit()
            ? R.menu.menu_master_product_edit
            : R.menu.menu_master_product_create,
        menuItem -> {
          if (menuItem.getItemId() == R.id.action_delete) {
            setForDestination(
                R.id.masterProductFragment,
                Constants.ARGUMENT.ACTION,
                Constants.ACTION.DELETE
            );
            activity.onBackPressed();
            return true;
          }
          if (menuItem.getItemId() == R.id.action_save_not_close) {
            setForDestination(
                R.id.masterProductFragment,
                Constants.ARGUMENT.ACTION,
                ACTION.SAVE_NOT_CLOSE
            );
            activity.onBackPressed();
            return true;
          }
          return false;
        }
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save_close,
        Constants.FAB.TAG.SAVE,
        animated,
        () -> {
          setForDestination(
              R.id.masterProductFragment,
              Constants.ARGUMENT.ACTION,
              ACTION.SAVE_CLOSE
          );
          activity.onBackPressed();
        }
    );
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
  }

  public void showInputNumberBottomSheet(int type) {
    Bundle bundle = new Bundle();
    bundle.putInt(FormDataMasterProductCatDueDate.DUE_DAYS_ARG, type);
    bundle.putInt(Constants.ARGUMENT.NUMBER, viewModel.getFormData().getDaysNumber(type));
    String hint = null;
    if (type == FormDataMasterProductCatDueDate.DUE_DAYS) {
      hint = getString(R.string.property_due_days_default);
    } else if (type == FormDataMasterProductCatDueDate.DUE_DAYS_OPENED) {
      hint = getString(R.string.property_due_days_default_opened);
    } else if (type == FormDataMasterProductCatDueDate.DUE_DAYS_FREEZING) {
      hint = getString(R.string.property_due_days_default_freezing);
    } else if (type == FormDataMasterProductCatDueDate.DUE_DAYS_THAWING) {
      hint = getString(R.string.property_due_days_default_thawing);
    }
    bundle.putString(ARGUMENT.HINT, hint);
    activity.showBottomSheet(new InputBottomSheet(), bundle);
  }

  public boolean showInputDateBottomSheet(int type) {
    Bundle bundle = new Bundle();
    bundle.putInt(FormDataMasterProductCatDueDate.DUE_DAYS_ARG, type);
    bundle.putInt(DateBottomSheet.DATE_TYPE, DateBottomSheet.DUE_DAYS_DEFAULT);
    bundle.putString(
        ARGUMENT.DEFAULT_DAYS_FROM_NOW,
        String.valueOf(viewModel.getFormData().getDaysNumber(type))
    );
    activity.showBottomSheet(new DateBottomSheet(), bundle);
    return true;
  }

  @Override
  public void saveInput(String text, Bundle argsBundle) {
    viewModel.getFormData().setDaysNumber(text, argsBundle);
  }

  @Override
  public boolean onBackPressed() {
    setForDestination(
        R.id.masterProductFragment,
        Constants.ARGUMENT.PRODUCT,
        viewModel.getFilledProduct()
    );
    return false;
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
