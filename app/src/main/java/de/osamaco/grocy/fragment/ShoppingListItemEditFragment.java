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
import android.os.Handler;
import android.text.InputType;
import android.view.FocusFinder;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentShoppingListItemEditBinding;
import de.osamaco.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import de.osamaco.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import de.osamaco.grocy.helper.InfoFullscreenHelper;
import de.osamaco.grocy.model.BottomSheetEvent;
import de.osamaco.grocy.model.Event;
import de.osamaco.grocy.model.InfoFullscreen;
import de.osamaco.grocy.model.Product;
import de.osamaco.grocy.model.QuantityUnit;
import de.osamaco.grocy.model.ShoppingList;
import de.osamaco.grocy.model.SnackbarMessage;
import de.osamaco.grocy.scanner.EmbeddedFragmentScanner;
import de.osamaco.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import de.osamaco.grocy.scanner.EmbeddedFragmentScannerBundle;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.Constants.ACTION;
import de.osamaco.grocy.util.Constants.ARGUMENT;
import de.osamaco.grocy.util.NumUtil;
import de.osamaco.grocy.util.ViewUtil;
import de.osamaco.grocy.viewmodel.ShoppingListItemEditViewModel;

public class ShoppingListItemEditFragment extends BaseFragment implements BarcodeListener {

  private final static String TAG = ShoppingListItemEditFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentShoppingListItemEditBinding binding;
  private ShoppingListItemEditViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup group, Bundle state) {
    binding = FragmentShoppingListItemEditBinding.inflate(inflater, group, false);
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
        this,
        binding.containerScanner,
        this
    );
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
    ShoppingListItemEditFragmentArgs args = ShoppingListItemEditFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new ShoppingListItemEditViewModel
        .ShoppingListItemEditViewModelFactory(activity.getApplication(), args)
    ).get(ShoppingListItemEditViewModel.class);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFormData(viewModel.getFormData());
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

    Integer productIdSavedSate = (Integer) getFromThisDestinationNow(Constants.ARGUMENT.PRODUCT_ID);
    if (productIdSavedSate != null) {
      removeForThisDestination(Constants.ARGUMENT.PRODUCT_ID);
      viewModel.setQueueEmptyAction(() -> viewModel.setProduct(productIdSavedSate));
    } else if (NumUtil.isStringInt(args.getProductId())) {
      int productId = Integer.parseInt(args.getProductId());
      setArguments(new ShoppingListItemEditFragmentArgs.Builder(args)
          .setProductId(null).build().toBundle());
      viewModel.setQueueEmptyAction(() -> viewModel.setProduct(productId));
    } else if (savedInstanceState == null && args.getAction().equals(ACTION.CREATE)) {
      if (binding.autoCompleteProduct.getText() == null
          || binding.autoCompleteProduct.getText().length() == 0) {
        new Handler().postDelayed(
            () -> activity.showKeyboard(binding.autoCompleteProduct),
            50
        );
      }
    }

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

    embeddedFragmentScanner.setScannerVisibilityLive(
        viewModel.getFormData().getScannerVisibilityLive()
    );

    viewModel.getFormData().getUseMultilineNoteLive().observe(getViewLifecycleOwner(), multi -> {
      if(multi) {
        binding.editTextNote.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        binding.editTextNote.setImeOptions(EditorInfo.IME_ACTION_UNSPECIFIED);
      } else {
        binding.editTextNote.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        binding.editTextNote.setImeOptions(EditorInfo.IME_ACTION_DONE);
      }
      if(binding.editTextNote.isFocused()) {
        activity.hideKeyboard();
        if (binding.editTextNote.getText() != null) {
          binding.editTextNote.setSelection(binding.editTextNote.getText().length());
        }
        binding.editTextNote.clearFocus();
        activity.showKeyboard(binding.editTextNote);
      }
    });

    // necessary because else getValue() doesn't give current value (?)
    viewModel.getFormData().getQuantityUnitsLive().observe(getViewLifecycleOwner(), qUs -> {
    });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    updateUI(args.getAnimateStart() && savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll_shopping_list_item_edit);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.END,
        viewModel.isActionEdit()
            ? R.menu.menu_shopping_list_item_edit_edit
            : R.menu.menu_shopping_list_item_edit_create,
        this::onMenuItemClick
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        animated,
        () -> {
          if (!viewModel.getFormData().isProductNameValid()) {
            clearFocusAndCheckProductInput();
          } else {
            viewModel.saveItem();
          }
        }
    );
  }

  @Override
  public void onResume() {
    super.onResume();
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (embeddedFragmentScanner != null) embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    clearInputFocus();
    viewModel.getFormData().toggleScannerVisibility();
    viewModel.onBarcodeRecognized(rawValue);
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }

  public void toggleScannerVisibility() {
    viewModel.getFormData().toggleScannerVisibility();
    if (viewModel.getFormData().isScannerVisible()) {
      clearInputFocus();
    }
  }

  public void clearAmountFieldAndFocusIt() {
    binding.editTextAmount.setText("");
    activity.showKeyboard(binding.editTextAmount);
  }

  public void saveItemOrClearInputFocus() {
    if (viewModel.getFormData().isFormValid()) {
      viewModel.saveItem();
    } else {
      clearInputFocus();
    }
  }

  public void clearFocusAndCheckProductInput() {
    clearInputFocus();
    viewModel.checkProductInput();
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.dummyFocusView.requestFocus();
    binding.textInputProduct.clearFocus();
    binding.textInputAmount.clearFocus();
    binding.textInputNote.clearFocus();
    binding.shoppingListContainer.clearFocus();
    binding.quantityUnitContainer.clearFocus();
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    Product product = (Product) adapterView.getItemAtPosition(pos);
    viewModel.setProduct(product);
    focusNextView();
  }

  public void onProductInputNextClick() {
    viewModel.checkProductInput();
    focusNextView();
  }

  public void focusNextView() {
    View nextView = FocusFinder.getInstance()
        .findNextFocus(binding.container, activity.getCurrentFocus(), View.FOCUS_DOWN);
    if (nextView == null) {
      clearInputFocus();
      return;
    }
    if (nextView.getId() == R.id.quantity_unit_container
        && viewModel.getFormData().getQuantityUnitsLive().getValue() != null
        && viewModel.getFormData().getQuantityUnitsLive().getValue().size() <= 1
    ) {
      nextView = binding.container.findViewById(R.id.edit_text_amount);
    }
    nextView.requestFocus();
    if (nextView instanceof EditText) {
      activity.showKeyboard((EditText) nextView);
    }
  }

  @Override
  public void onBottomSheetDismissed() {
    focusNextView();
  }

  public void showShoppingListsBottomSheet() {
    activity.showBottomSheet(new ShoppingListsBottomSheet());
  }

  public void showQuantityUnitsBottomSheet(boolean hasFocus) {
    if (!hasFocus) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(
        Constants.ARGUMENT.QUANTITY_UNITS,
        viewModel.getFormData().getQuantityUnitsLive().getValue()
    );
    QuantityUnit quantityUnit = viewModel.getFormData().getQuantityUnitLive().getValue();
    bundle.putInt(ARGUMENT.SELECTED_ID, quantityUnit != null ? quantityUnit.getId() : -1);
    activity.showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
  }

  @Nullable
  @Override
  public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
    return viewModel.getFormData().getShoppingListIdLive();
  }

  @Override
  public void selectShoppingList(ShoppingList shoppingList) {
    viewModel.getFormData().getShoppingListLive().setValue(shoppingList);
  }

  @Override
  public void selectQuantityUnit(QuantityUnit quantityUnit) {
    viewModel.getFormData().getQuantityUnitLive().setValue(quantityUnit);
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_delete) {
      ViewUtil.startIcon(item);
      viewModel.deleteItem();
      return true;
    } else if (item.getItemId() == R.id.action_product_overview) {
      ViewUtil.startIcon(item);
      viewModel.showProductDetailsBottomSheet();
      return true;
    } else if (item.getItemId() == R.id.action_clear_form) {
      clearInputFocus();
      viewModel.getFormData().clearForm();
      return true;
    }
    return false;
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
