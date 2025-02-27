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

package xyz.zedler.patrick.grocy.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.Timer;
import java.util.TimerTask;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingModeItemAdapter;
import xyz.zedler.patrick.grocy.adapter.ShoppingPlaceholderAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingModeBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextEditBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.SHOPPING_MODE;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingModeViewModel;

public class ShoppingModeFragment extends BaseFragment implements
    ShoppingModeItemAdapter.ShoppingModeItemClickListener {

  private final static String TAG = ShoppingModeFragment.class.getSimpleName();

  private MainActivity activity;
  private SharedPreferences sharedPrefs;
  private ShoppingModeViewModel viewModel;
  private ClickUtil clickUtil;
  private FragmentShoppingModeBinding binding;
  private InfoFullscreenHelper infoFullscreenHelper;
  private Timer timer;
  private TimerTask timerTask;
  private Handler handler;

  private boolean debug = false;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentShoppingModeBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (infoFullscreenHelper != null) {
      infoFullscreenHelper.destroyInstance();
      infoFullscreenHelper = null;
    }
    if (binding != null) {
      binding.recycler.animate().cancel();
      binding.buttonShoppingListLists.animate().cancel();
      binding.textShoppingListTitle.animate().cancel();
      binding.recycler.setAdapter(null);
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(this).get(ShoppingModeViewModel.class);
    viewModel.setOfflineLive(!activity.isOnline());
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
    clickUtil = new ClickUtil();
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    handler = new Handler();

    if (savedInstanceState == null) {
      binding.recycler.scrollTo(0, 0);
    }

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new ShoppingPlaceholderAdapter());

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), state -> {
      if (!state) {
        viewModel.setCurrentQueueLoading(null);
      }
    });

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getSelectedShoppingListIdLive().observe(
        getViewLifecycleOwner(), this::changeAppBarTitle
    );

    viewModel.getFilteredShoppingListItemsLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) {
        return;
      }
      if (items.isEmpty()) {
        InfoFullscreen info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_SHOPPING_LIST);
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      if (binding.recycler.getAdapter() instanceof ShoppingModeItemAdapter) {
        ((ShoppingModeItemAdapter) binding.recycler.getAdapter()).updateData(
            requireContext(),
            items,
            viewModel.getProductHashMap(),
            viewModel.getProductNamesHashMap(),
            viewModel.getQuantityUnitHashMap(),
            viewModel.getProductGroupHashMap(),
            viewModel.getStoreHashMap(),
            viewModel.getShoppingListItemAmountsHashMap(),
            viewModel.getMissingProductIds(),
            viewModel.getShoppingListNotes(),
            viewModel.getGroupingMode()
        );
      } else {
        binding.recycler.setAdapter(
            new ShoppingModeItemAdapter(
                requireContext(),
                (LinearLayoutManager) binding.recycler.getLayoutManager(),
                items,
                viewModel.getProductHashMap(),
                viewModel.getProductNamesHashMap(),
                viewModel.getQuantityUnitHashMap(),
                viewModel.getProductGroupHashMap(),
                viewModel.getStoreHashMap(),
                viewModel.getShoppingListItemAmountsHashMap(),
                viewModel.getMissingProductIds(),
                this,
                viewModel.getShoppingListNotes(),
                viewModel.getGroupingMode(),
                sharedPrefs.getBoolean(
                    SHOPPING_MODE.USE_SMALLER_FONT,
                    SETTINGS_DEFAULT.SHOPPING_MODE.USE_SMALLER_FONT
                ),
                sharedPrefs.getBoolean(
                    SHOPPING_MODE.SHOW_PRODUCT_DESCRIPTION,
                    SETTINGS_DEFAULT.SHOPPING_MODE.SHOW_PRODUCT_DESCRIPTION
                ),
                sharedPrefs.getBoolean(
                    Constants.SETTINGS.SHOPPING_MODE.SHOW_DONE_ITEMS,
                    Constants.SETTINGS_DEFAULT.SHOPPING_MODE.SHOW_DONE_ITEMS
                )
            )
        );
        binding.recycler.scheduleLayoutAnimation();
      }
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.frameMainContainer
        ));
      }
    });

    hideDisabledFeatures();

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    updateUI();
  }

  @Override
  public void onStart() {
    super.onStart();
    keepScreenOnIfNecessary(true);
  }

  @Override
  public void onStop() {
    super.onStop();
    keepScreenOnIfNecessary(false);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (timer != null) {
      timer.cancel();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    int seconds = sharedPrefs.getInt(
        Constants.SETTINGS.SHOPPING_MODE.UPDATE_INTERVAL,
        Constants.SETTINGS_DEFAULT.SHOPPING_MODE.UPDATE_INTERVAL
    );
    if (seconds == 0) {
      return;
    }
    timer = new Timer();
    initTimerTask();
    timer.schedule(timerTask, 2000, seconds * 1000L);
  }

  private void updateUI() {
    activity.getScrollBehavior().setUpScroll(null);
    activity.getScrollBehavior().setHideOnScroll(false);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.GONE,
        R.menu.menu_shopping_list,
        () -> {
        }
    );
  }

  @Override
  public void selectShoppingList(ShoppingList shoppingList) {
    viewModel.selectShoppingList(shoppingList);
  }

  private void changeAppBarTitle(int selectedShoppingListId) {
    ShoppingList shoppingList = viewModel.getShoppingListFromId(selectedShoppingListId);
    if (shoppingList == null) {
      return;
    }
    ShoppingListFragment.changeAppBarTitle(
        binding.textShoppingListTitle,
        binding.buttonShoppingListLists,
        shoppingList
    );
  }

  public void toggleDoneStatus(ShoppingListItem shoppingListItem) {
    viewModel.toggleDoneStatus(shoppingListItem);
  }

  @Override
  public void saveText(Spanned notes) {
    viewModel.saveNotes(notes);
  }

  private void showNotesEditor() {
    Bundle bundle = new Bundle();
    bundle.putString(
        Constants.ARGUMENT.TITLE,
        activity.getString(R.string.action_edit_notes)
    );
    bundle.putString(
        Constants.ARGUMENT.HINT,
        activity.getString(R.string.property_notes)
    );
    ShoppingList shoppingList = viewModel.getSelectedShoppingList();
    if (shoppingList == null) {
      return;
    }
    bundle.putString(Constants.ARGUMENT.HTML, shoppingList.getNotes());
    activity.showBottomSheet(new TextEditBottomSheet(), bundle);
  }

  public void showShoppingListsBottomSheet() {
    activity.showBottomSheet(new ShoppingListsBottomSheet());
  }

  @Override
  public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
    return viewModel.getSelectedShoppingListIdLive();
  }

  @Override
  public void onItemRowClicked(GroupedListItem groupedListItem) {
    if (clickUtil.isDisabled()) {
      return;
    }
    if (groupedListItem == null) {
      return;
    }
    if (groupedListItem.getType(GroupedListItem.CONTEXT_SHOPPING_LIST)
        == GroupedListItem.TYPE_ENTRY) {
      toggleDoneStatus((ShoppingListItem) groupedListItem);
    } else if (!viewModel.isOffline()
        && groupedListItem.getType(GroupedListItem.CONTEXT_SHOPPING_LIST)
        == GroupedListItem.TYPE_BOTTOM_NOTES) {  // Click on bottom notes
      showNotesEditor();
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

  private void hideDisabledFeatures() {
    if (isFeatureMultipleListsDisabled()) {
      binding.buttonShoppingListLists.setVisibility(View.GONE);
      binding.textShoppingListTitle.setOnClickListener(null);
    }
  }

  private boolean isFeatureMultipleListsDisabled() {
    return !sharedPrefs.getBoolean(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true);
  }

  private void initTimerTask() {
    if (timerTask != null) {
      timerTask.cancel();
    }
    timerTask = new TimerTask() {
      @Override
      public void run() {
        if (debug) {
          Log.i(TAG, "auto sync shopping list (but may skip download)");
        }
        handler.post(() -> viewModel.downloadData());
      }
    };
  }

  private void keepScreenOnIfNecessary(boolean keepOn) {
    if (activity == null) {
      activity = (MainActivity) requireActivity();
    }
    if (sharedPrefs == null) {
      sharedPrefs = PreferenceManager
          .getDefaultSharedPreferences(activity);
    }
    boolean necessary = sharedPrefs.getBoolean(
        Constants.SETTINGS.SHOPPING_MODE.KEEP_SCREEN_ON,
        Constants.SETTINGS_DEFAULT.SHOPPING_MODE.KEEP_SCREEN_ON
    );
    if (necessary && keepOn) {
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}