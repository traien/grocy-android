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

package de.osamaco.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentBottomsheetDrawerBinding;
import de.osamaco.grocy.fragment.BaseFragment;
import de.osamaco.grocy.fragment.ConsumeFragment;
import de.osamaco.grocy.fragment.InventoryFragment;
import de.osamaco.grocy.fragment.MasterObjectListFragment;
import de.osamaco.grocy.fragment.OverviewStartFragment;
import de.osamaco.grocy.fragment.PurchaseFragment;
import de.osamaco.grocy.fragment.SettingsFragment;
import de.osamaco.grocy.fragment.ShoppingListFragment;
import de.osamaco.grocy.fragment.StockOverviewFragment;
import de.osamaco.grocy.fragment.TransferFragment;
import de.osamaco.grocy.util.ClickUtil;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.Constants.PREF;
import de.osamaco.grocy.util.NetUtil;

public class DrawerBottomSheet extends BaseBottomSheet implements View.OnClickListener {

  private final static String TAG = DrawerBottomSheet.class.getSimpleName();

  private FragmentBottomsheetDrawerBinding binding;
  private MainActivity activity;
  private SharedPreferences sharedPrefs;
  private final ClickUtil clickUtil = new ClickUtil();

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetDrawerBinding.inflate(
        inflater, container, false
    );

    activity = (MainActivity) requireActivity();

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

    binding.buttonDrawerShoppingMode.setOnClickListener(
        v -> navigateDeepLink(R.string.deep_link_shoppingModeFragment)
    );

    ClickUtil.setOnClickListeners(
        this,
        binding.linearDrawerStock,
        binding.linearDrawerShoppingList,
        binding.linearDrawerConsume,
        binding.linearDrawerPurchase,
        binding.linearDrawerTransfer,
        binding.linearDrawerInventory,
        binding.linearDrawerMasterData,
        binding.linearDrawerSettings,
        binding.linearDrawerFeedback,
        binding.linearDrawerHelp
    );

    BaseFragment currentFragment = activity.getCurrentFragment();
    if (currentFragment instanceof StockOverviewFragment) {
      select(binding.linearDrawerStock, binding.textDrawerStock, false);
    } else if (currentFragment instanceof ShoppingListFragment) {
      select(binding.linearDrawerShoppingList, binding.textDrawerShoppingList, false);
    } else if (currentFragment instanceof ConsumeFragment) {
      select(binding.linearDrawerConsume, null, true);
    } else if (currentFragment instanceof PurchaseFragment) {
      select(binding.linearDrawerPurchase, null, true);
    } else if (currentFragment instanceof TransferFragment) {
      select(binding.linearDrawerTransfer, null, true);
    } else if (currentFragment instanceof InventoryFragment) {
      select(binding.linearDrawerInventory, null, true);
    } else if (currentFragment instanceof MasterObjectListFragment) {
      select(binding.linearDrawerMasterData, binding.textDrawerMasterData, false);
    } else if (currentFragment instanceof SettingsFragment) {
      select(binding.linearDrawerSettings, binding.textDrawerSettings, false);
    }

    hideDisabledFeatures();

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  public void onClick(View v) {
    if (clickUtil.isDisabled()) {
      return;
    }

    if (v.getId() == R.id.linear_drawer_stock) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToStockOverviewFragment());
    } else if (v.getId() == R.id.linear_drawer_shopping_list) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToShoppingListFragment());
    } else if (v.getId() == R.id.linear_drawer_consume) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToConsumeFragment());
    } else if (v.getId() == R.id.linear_drawer_purchase) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToPurchaseFragment());
    } else if (v.getId() == R.id.linear_drawer_transfer) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToTransferFragment());
    } else if (v.getId() == R.id.linear_drawer_inventory) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToInventoryFragment());
    } else if (v.getId() == R.id.linear_drawer_master_data) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToNavigationMasterObjects());
    } else if (v.getId() == R.id.linear_drawer_settings) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToSettingsFragment());
    } else if (v.getId() == R.id.linear_drawer_feedback) {
      activity.showBottomSheet(new FeedbackBottomSheet());
      dismiss();
    } else if (v.getId() == R.id.linear_drawer_help) {
      if (!NetUtil.openURL(activity, Constants.URL.HELP)) {
        activity.showMessage(R.string.error_no_browser);
      }
      dismiss();
    }
  }

  private void navigateCustom(NavDirections directions) {
    NavOptions.Builder builder = new NavOptions.Builder();
    builder.setEnterAnim(R.anim.slide_in_up).setPopExitAnim(R.anim.slide_out_down);
    builder.setPopUpTo(R.id.overviewStartFragment, false);
    if (!(activity.getCurrentFragment() instanceof OverviewStartFragment)) {
      builder.setExitAnim(R.anim.slide_out_down);
    } else {
      builder.setExitAnim(R.anim.slide_no);
    }
    dismiss();
    navigate(directions, builder.build());
  }

  @Override
  void navigateDeepLink(@StringRes int uri) {
    NavOptions.Builder builder = new NavOptions.Builder();
    builder.setEnterAnim(R.anim.slide_in_up).setPopExitAnim(R.anim.slide_out_down);
    builder.setPopUpTo(R.id.overviewStartFragment, false);
    if (!(activity.getCurrentFragment() instanceof OverviewStartFragment)) {
      builder.setExitAnim(R.anim.slide_out_down);
    } else {
      builder.setExitAnim(R.anim.slide_no);
    }
    dismiss();
    findNavController().navigate(Uri.parse(getString(uri)), builder.build());
  }

  private void select(LinearLayout linearLayout, TextView textView, boolean multiRowItem) {
    linearLayout.setBackgroundResource(
        multiRowItem
            ? R.drawable.bg_drawer_item_multirow_selected
            : R.drawable.bg_drawer_item_selected
    );
    linearLayout.setClickable(false);
    if (textView != null) {
      textView.setTextColor(ContextCompat.getColor(activity, R.color.retro_green_fg));
    }
  }

  private void hideDisabledFeatures() {
    if (!isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
      binding.frameShoppingList.setVisibility(View.GONE);
    }
    if (!isFeatureEnabled(PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      binding.linearDrawerTransfer.setVisibility(View.GONE);
      binding.transactionsContainer.setWeightSum(75f);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
