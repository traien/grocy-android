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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentBottomsheetInputProductBinding;
import de.osamaco.grocy.fragment.MasterProductFragmentArgs;
import de.osamaco.grocy.util.Constants;

public class InputProductBottomSheet extends BaseBottomSheet {

  private final static String TAG = InputProductBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetInputProductBinding binding;

  private MutableLiveData<Integer> selectionLive;

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
    binding = FragmentBottomsheetInputProductBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    setSkipCollapsedInPortrait();
    setCancelable(false);
    super.onViewCreated(view, savedInstanceState);
    activity = (MainActivity) requireActivity();
    binding.setBottomsheet(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    String input = requireArguments().getString(Constants.ARGUMENT.PRODUCT_INPUT);
    assert input != null;
    binding.input.setText(input);

    boolean stringOnlyContainsNumbers = true;
    for (char c : input.trim().toCharArray()) {
      try {
        Integer.parseInt(String.valueOf(c));
      } catch (NumberFormatException e) {
        stringOnlyContainsNumbers = false;
        break;
      }
    }
    selectionLive = new MutableLiveData<>(stringOnlyContainsNumbers ? 3 : 1);
  }

  public void proceed() {
    assert selectionLive.getValue() != null;
    String input = binding.input.getText().toString();
    if (selectionLive.getValue() == 1) {
      navigateDeepLink(R.string.deep_link_masterProductFragment,
          new MasterProductFragmentArgs.Builder(Constants.ACTION.CREATE)
              .setProductName(input).build().toBundle());
    } else if (selectionLive.getValue() == 2) {
      activity.getCurrentFragment().addBarcodeToNewProduct(input.trim());
      navigateDeepLink(R.string.deep_link_masterProductFragment,
          new MasterProductFragmentArgs.Builder(Constants.ACTION.CREATE)
              .build().toBundle());
    } else {
      activity.getCurrentFragment().addBarcodeToExistingProduct(input.trim());
    }
    dismiss();
  }

  public MutableLiveData<Integer> getSelectionLive() {
    return selectionLive;
  }

  public void setSelectionLive(int selection) {
    selectionLive.setValue(selection);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
