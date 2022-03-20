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

import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentShoppingListEditBinding;
import de.osamaco.grocy.helper.InfoFullscreenHelper;
import de.osamaco.grocy.model.Event;
import de.osamaco.grocy.model.InfoFullscreen;
import de.osamaco.grocy.model.ShoppingList;
import de.osamaco.grocy.model.SnackbarMessage;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.viewmodel.ShoppingListEditViewModel;

public class ShoppingListEditFragment extends BaseFragment {

  private final static String TAG = ShoppingListEditFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentShoppingListEditBinding binding;
  private ShoppingListEditViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private ShoppingList startUpShoppingList;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentShoppingListEditBinding.inflate(inflater, container, false);
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
    startUpShoppingList = ShoppingListEditFragmentArgs
        .fromBundle(requireArguments()).getShoppingList();
    viewModel = new ViewModelProvider(this, new ShoppingListEditViewModel
        .ShoppingListEditViewModelFactory(
        activity.getApplication(), startUpShoppingList
    )).get(ShoppingListEditViewModel.class);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.frameMainContainer
        ));
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navigateUp();
      } else if (event.getType() == Event.SET_SHOPPING_LIST_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
        setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isLoading -> {
      if (!isLoading) {
        viewModel.setCurrentQueueLoading(null);
      }
    });

    viewModel.getOfflineLive().observe(getViewLifecycleOwner(), offline -> {
      InfoFullscreen infoFullscreen = offline ? new InfoFullscreen(
          InfoFullscreen.ERROR_OFFLINE,
          () -> updateConnectivity(true)
      ) : null;
      viewModel.getInfoFullscreenLive().setValue(infoFullscreen);
    });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    if (startUpShoppingList == null && savedInstanceState == null) {
      new Handler().postDelayed(
          () -> activity.showKeyboard(binding.editTextName),
          50
      );
    }

    updateUI(ShoppingListEditFragmentArgs.fromBundle(requireArguments())
        .getAnimateStart() && savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.END,
        R.menu.menu_shopping_list_edit,
        this::setUpBottomMenu
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        animated,
        () -> {
          clearInputFocus();
          viewModel.saveShoppingList();
        }
    );
  }

  private void clearInputFocus() {
    activity.hideKeyboard();
    binding.textInputName.clearFocus();
  }

  public void setUpBottomMenu() {
    MenuItem menuItemDelete;
    menuItemDelete = activity.getBottomMenu().findItem(R.id.action_delete);
    if (menuItemDelete != null && startUpShoppingList != null && startUpShoppingList.getId() != 1) {
      menuItemDelete.setVisible(true);
      menuItemDelete.setOnMenuItemClickListener(item -> {
        ((Animatable) menuItemDelete.getIcon()).start();
        viewModel.safeDeleteShoppingList();
        return true;
      });
    }
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
    if (isOnline) {
      viewModel.downloadData();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
