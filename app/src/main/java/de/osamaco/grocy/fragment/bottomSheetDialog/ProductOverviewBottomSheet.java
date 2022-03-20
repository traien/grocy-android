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

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuCompat;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentBottomsheetProductOverviewBinding;
import de.osamaco.grocy.fragment.MasterProductFragmentArgs;
import de.osamaco.grocy.fragment.ShoppingListItemEditFragmentArgs;
import de.osamaco.grocy.helper.DownloadHelper;
import de.osamaco.grocy.model.Location;
import de.osamaco.grocy.model.PriceHistoryEntry;
import de.osamaco.grocy.model.Product;
import de.osamaco.grocy.model.ProductDetails;
import de.osamaco.grocy.model.QuantityUnit;
import de.osamaco.grocy.model.StockItem;
import de.osamaco.grocy.model.Store;
import de.osamaco.grocy.util.AmountUtil;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.Constants.PREF;
import de.osamaco.grocy.util.DateUtil;
import de.osamaco.grocy.util.NumUtil;
import de.osamaco.grocy.util.PluralUtil;
import de.osamaco.grocy.util.TextUtil;
import de.osamaco.grocy.view.BezierCurveChart;

public class ProductOverviewBottomSheet extends BaseBottomSheet {

  private final static String TAG = ProductOverviewBottomSheet.class.getSimpleName();

  private SharedPreferences sharedPrefs;
  private MainActivity activity;
  private FragmentBottomsheetProductOverviewBinding binding;
  private StockItem stockItem;
  private ProductDetails productDetails;
  private Product product;
  private QuantityUnit quantityUnit;
  private PluralUtil pluralUtil;
  private Location location;
  private DownloadHelper dlHelper;

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
    binding = FragmentBottomsheetProductOverviewBinding
        .inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    activity = (MainActivity) requireActivity();
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    pluralUtil = new PluralUtil(activity);

    ProductOverviewBottomSheetArgs args =
        ProductOverviewBottomSheetArgs.fromBundle(requireArguments());

    boolean showActions = args.getShowActions();

    // setup in CONSUME/PURCHASE with ProductDetails, in STOCK with StockItem

    if (args.getProductDetails() != null) {
      productDetails = args.getProductDetails();
      product = productDetails.getProduct();
      stockItem = new StockItem(productDetails);
    } else if (args.getStockItem() != null) {
      stockItem = args.getStockItem();
      quantityUnit = args.getQuantityUnit();
      location = args.getLocation();
      product = stockItem.getProduct();
    }

    // WEB REQUESTS

    dlHelper = new DownloadHelper(activity, TAG);

    // VIEWS

    refreshItems();

    binding.textName.setText(product.getName());

    // TOOLBAR

    boolean isInStock = stockItem.getAmountDouble() > 0;
    MenuCompat.setGroupDividerEnabled(binding.toolbar.getMenu(), true);
    // disable actions if necessary
    binding.toolbar.getMenu().findItem(R.id.action_consume_all).setEnabled(isInStock);
    binding.toolbar.getMenu().findItem(R.id.action_consume_spoiled).setEnabled(
        isInStock && product.getEnableTareWeightHandlingInt() == 0
    );
    binding.toolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_add_to_shopping_list) {
				navigateDeepLink(R.string.deep_link_shoppingListItemEditFragment,
						new ShoppingListItemEditFragmentArgs.Builder(Constants.ACTION.CREATE)
								.setProductId(String.valueOf(product.getId())).build().toBundle());
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_consume_all) {
        activity.getCurrentFragment().performAction(
            Constants.ACTION.CONSUME_ALL,
            stockItem
        );
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_consume_spoiled) {
        activity.getCurrentFragment().performAction(
            Constants.ACTION.CONSUME_SPOILED,
            stockItem
        );
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_edit_product) {
        String productId = String.valueOf(product.getId());
        navigateDeepLink(R.string.deep_link_masterProductFragment,
            new MasterProductFragmentArgs.Builder(Constants.ACTION.EDIT)
                .setProductId(productId).build().toBundle());
        dismiss();
        return true;
      }
      return false;
    });

    Chip chipConsume = view.findViewById(R.id.chip_consume);
    chipConsume.setVisibility(isInStock ? View.VISIBLE : View.GONE);
    chipConsume.setOnClickListener(v -> {
      NavHostFragment.findNavController(this).navigate(
          ProductOverviewBottomSheetDirections
              .actionProductOverviewBottomSheetDialogFragmentToConsumeFragment()
              .setCloseWhenFinished(true)
              .setProductId(String.valueOf(product.getId()))
      );
      dismiss();
    });

    Chip chipPurchase = view.findViewById(R.id.chip_purchase);
    chipPurchase.setOnClickListener(v -> {
      NavHostFragment.findNavController(this).navigate(
          ProductOverviewBottomSheetDirections
              .actionProductOverviewBottomSheetDialogFragmentToPurchaseFragment()
              .setCloseWhenFinished(true)
              .setProductId(String.valueOf(product.getId()))
      );
      dismiss();
    });

    Chip chipTransfer = view.findViewById(R.id.chip_transfer);
    chipTransfer.setVisibility(isInStock && product.getEnableTareWeightHandlingInt() == 0
        ? View.VISIBLE : View.GONE);
    chipTransfer.setOnClickListener(v -> {
      NavHostFragment.findNavController(this).navigate(
          ProductOverviewBottomSheetDirections
              .actionProductOverviewBottomSheetDialogFragmentToTransferFragment()
              .setCloseWhenFinished(true)
              .setProductId(String.valueOf(product.getId()))
      );
      dismiss();
    });

    Chip chipInventory = view.findViewById(R.id.chip_inventory);
    chipInventory.setOnClickListener(v -> {
      NavHostFragment.findNavController(this).navigate(
          ProductOverviewBottomSheetDirections
              .actionProductOverviewBottomSheetDialogFragmentToInventoryFragment()
              .setCloseWhenFinished(true)
              .setProductId(String.valueOf(product.getId()))
      );
      dismiss();
    });

    if (!showActions) {
      view.findViewById(R.id.container_chips).setVisibility(View.GONE);
    }

    // DESCRIPTION

    Spanned description = product.getDescription() != null
        ? Html.fromHtml(product.getDescription())
        : null;
    description = (Spanned) TextUtil.trimCharSequence(description);
    if (description != null && !description.toString().isEmpty()) {
      binding.cardDescription.setText(description.toString());
    } else {
      binding.cardDescription.setVisibility(View.GONE);
    }

    // ACTIONS

    if (!showActions) {
      // hide actions when set up with productDetails
      binding.linearActionContainer.setVisibility(View.GONE);
      // set info menu
      binding.toolbar.getMenu().clear();
      binding.toolbar.inflateMenu(R.menu.menu_actions_product_overview_info);
    }

    refreshButtonStates(false);
    binding.buttonConsume.setOnClickListener(v -> {
      disableActions();
      activity.getCurrentFragment().performAction(Constants.ACTION.CONSUME, stockItem);
      dismiss();
    });
    binding.buttonOpen.setOnClickListener(v -> {
      disableActions();
      activity.getCurrentFragment().performAction(Constants.ACTION.OPEN, stockItem);
      dismiss();
    });
    // tooltips
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      binding.buttonConsume.setTooltipText(
          activity.getString(
              R.string.action_consume_one,
              quantityUnit.getName(),
              product.getName()
          )
      );
      binding.buttonOpen.setTooltipText(
          activity.getString(
              R.string.action_open_one,
              quantityUnit.getName(),
              product.getName()
          )
      );
      // TODO: tooltip colors
    }
    // no margin if description is != null
    if (description != null) {
      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
      );
      layoutParams.setMargins(0, 0, 0, 0);
      binding.linearAmount.setLayoutParams(layoutParams);
    }

    // LOAD DETAILS

    if (activity.isOnline() && !hasDetails()) {
      dlHelper.getProductDetails(product.getId(), details -> {
        productDetails = details;
        stockItem = new StockItem(productDetails);
        refreshButtonStates(true);
        refreshItems();
        loadPriceHistory();
      }).perform(dlHelper.getUuid());
    } else if (activity.isOnline() && hasDetails()) {
      loadPriceHistory();
    }

    hideDisabledFeatures();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    dlHelper.destroy();
    binding = null;
  }

  private void refreshItems() {
    DateUtil dateUtil = new DateUtil(activity);

    // quantity unit refresh for an up-to-date value (productDetails has it in it)
    if (hasDetails()) {
      quantityUnit = productDetails.getQuantityUnitStock();
    }

    // AMOUNT
    StringBuilder amountNormal = new StringBuilder();
    StringBuilder amountAggregated = new StringBuilder();
    AmountUtil.addStockAmountNormalInfo(activity, pluralUtil, amountNormal, stockItem, quantityUnit);
    AmountUtil.addStockAmountAggregatedInfo(activity, pluralUtil, amountAggregated, stockItem, quantityUnit);
    binding.itemAmount.setText(
        activity.getString(R.string.property_amount),
        amountNormal.toString(),
        amountAggregated.toString().isEmpty() ? null : amountAggregated.toString().trim()
    );
    binding.itemAmount.setSingleLine(false);

    // LOCATION
    if (hasDetails()) {
      location = productDetails.getLocation(); // refresh
    }
    if (location != null && isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      binding.itemLocation.setText(
          activity.getString(R.string.property_location_default),
          location.getName(),
          null
      );
    } else {
      binding.itemLocation.setVisibility(View.GONE);
    }

    // BEST BEFORE
    if (isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
      String bestBefore = stockItem.getBestBeforeDate();
      if (bestBefore == null) {
        bestBefore = ""; // for "never" from dateUtil
      }
      binding.itemDueDate.setText(
          activity.getString(R.string.property_due_date_next),
          !bestBefore.equals(Constants.DATE.NEVER_OVERDUE)
              ? dateUtil.getLocalizedDate(bestBefore)
              : activity.getString(R.string.date_never),
          !bestBefore.equals(Constants.DATE.NEVER_OVERDUE) && !bestBefore.isEmpty()
              ? dateUtil.getHumanForDaysFromNow(bestBefore)
              : null
      );
    }

    if (hasDetails()) {
      // LAST PURCHASED
      String lastPurchased = productDetails.getLastPurchased();
      binding.itemLastPurchased.setText(
          activity.getString(R.string.property_last_purchased),
          lastPurchased != null
              ? dateUtil.getLocalizedDate(lastPurchased)
              : activity.getString(R.string.date_never),
          lastPurchased != null
              ? dateUtil.getHumanForDaysFromNow(lastPurchased)
              : null
      );

      // LAST USED
      String lastUsed = productDetails.getLastUsed();
      binding.itemLastUsed.setText(
          activity.getString(R.string.property_last_used),
          lastUsed != null
              ? dateUtil.getLocalizedDate(lastUsed)
              : activity.getString(R.string.date_never),
          lastUsed != null
              ? dateUtil.getHumanForDaysFromNow(lastUsed)
              : null
      );

      // LAST PRICE
      String lastPrice = productDetails.getLastPrice();
      if (NumUtil.isStringDouble(lastPrice) && isFeatureEnabled(
          Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
        binding.itemLastPrice.setText(
            activity.getString(R.string.property_last_price),
            NumUtil.trimPrice(Double.parseDouble(lastPrice))
                + " " + sharedPrefs.getString(Constants.PREF.CURRENCY, ""),
            null
        );
      }

      // SHELF LIFE
      int shelfLife = productDetails.getAverageShelfLifeDaysInt();
      if (shelfLife != 0 && shelfLife != -1 && isFeatureEnabled(
          Constants.PREF.FEATURE_STOCK_BBD_TRACKING
      )) {
        binding.itemShelfLife.setText(
            activity.getString(R.string.property_average_shelf_life),
            dateUtil.getHumanDuration(shelfLife),
            null
        );
      }

      // SPOIL RATE
      binding.itemSpoilRate.setText(
          activity.getString(R.string.property_spoil_rate),
          NumUtil.trim(productDetails.getSpoilRatePercent()) + "%",
          null
      );
    }
  }

  private void loadPriceHistory() {
    if (!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      return;
    }
    dlHelper.get(
        activity.getGrocyApi().getPriceHistory(product.getId()),
        response -> {
          Type listType = new TypeToken<ArrayList<PriceHistoryEntry>>() {
          }.getType();
          ArrayList<PriceHistoryEntry> priceHistoryEntries;
          priceHistoryEntries = new Gson().fromJson(response, listType);
          if (priceHistoryEntries.isEmpty()) {
            return;
          }

          ArrayList<String> dates = new ArrayList<>();
          Collections.reverse(priceHistoryEntries);

          HashMap<String, ArrayList<BezierCurveChart.Point>> curveLists = new HashMap<>();

          for (PriceHistoryEntry priceHistoryEntry : priceHistoryEntries) {
            Store store = priceHistoryEntry.getStore();
            String storeName;
            if (store == null || store.getName().trim().isEmpty()) {
              storeName = activity.getString(R.string.property_store_unknown);
            } else {
              storeName = store.getName().trim();
            }
            if (!curveLists.containsKey(storeName)) {
              curveLists.put(storeName, new ArrayList<>());
            }
            ArrayList<BezierCurveChart.Point> curveList = curveLists.get(storeName);

            String date = new DateUtil(activity).getLocalizedDate(
                priceHistoryEntry.getDate(),
                DateUtil.FORMAT_SHORT
            );
            if (!dates.contains(date)) {
              dates.add(date);
            }
            assert curveList != null;
            curveList.add(new BezierCurveChart.Point(
                dates.indexOf(date),
                (float) priceHistoryEntry.getPrice()
            ));
          }
          binding.itemPriceHistory.init(curveLists, dates);
          animateLinearPriceHistory();
        },
        error -> {
        }
    );
  }

  private void animateLinearPriceHistory() {
    LinearLayout linearPriceHistory = binding.linearPriceHistory;
    linearPriceHistory.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    int height = linearPriceHistory.getMeasuredHeight();
    linearPriceHistory.getLayoutParams().height = 0;
    linearPriceHistory.requestLayout();
    linearPriceHistory.setAlpha(0);
    linearPriceHistory.setVisibility(View.VISIBLE);
    linearPriceHistory.animate().alpha(1).setDuration(600).setStartDelay(100).start();
    linearPriceHistory.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            ValueAnimator heightAnimator = ValueAnimator.ofInt(0, height);
            heightAnimator.addUpdateListener(animation -> {
              linearPriceHistory.getLayoutParams().height = (int) animation
                  .getAnimatedValue();
              linearPriceHistory.requestLayout();
            });
            heightAnimator.setDuration(800).setInterpolator(
                new DecelerateInterpolator()
            );
            heightAnimator.start();
            if (linearPriceHistory.getViewTreeObserver().isAlive()) {
              linearPriceHistory.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        }
    );
  }

  private void refreshButtonStates(boolean animated) {
    boolean consume = stockItem.getAmountDouble() > 0
        && stockItem.getProduct().getEnableTareWeightHandlingInt() == 0;
    boolean open = stockItem.getAmountDouble() > stockItem.getAmountOpenedDouble()
        && stockItem.getProduct().getEnableTareWeightHandlingInt() == 0;
    if (animated) {
      binding.buttonConsume.refreshState(consume);
      binding.buttonOpen.refreshState(open);
    } else {
      binding.buttonConsume.setState(consume);
      binding.buttonOpen.setState(open);
    }
  }

  private void disableActions() {
    binding.buttonConsume.refreshState(false);
    binding.buttonOpen.refreshState(false);
  }

  private void hideDisabledFeatures() {
    if (!isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
      binding.toolbar.getMenu().findItem(R.id.action_add_to_shopping_list).setVisible(false);
    }
    if (!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
      binding.itemDueDate.setVisibility(View.GONE);
    }
    if (!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_OPENED_TRACKING)) {
      binding.buttonOpen.setVisibility(View.GONE);
    }
    if (!isFeatureEnabled(PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      binding.chipTransfer.setVisibility(View.GONE);
    }
  }

  private boolean hasDetails() {
    return productDetails != null;
  }

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
