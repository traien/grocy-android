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
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.adapter.QuantityUnitAdapter;
import de.osamaco.grocy.model.QuantityUnit;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.Constants.ARGUMENT;
import de.osamaco.grocy.util.SortUtil;

public class QuantityUnitsBottomSheet extends BaseBottomSheet
    implements QuantityUnitAdapter.QuantityUnitAdapterListener {

  private final static String TAG = QuantityUnitsBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private ArrayList<QuantityUnit> quantityUnits;

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
    View view = inflater.inflate(
        R.layout.fragment_bottomsheet_list_selection, container, false
    );

    activity = (MainActivity) requireActivity();
    Bundle bundle = requireArguments();

    ArrayList<QuantityUnit> qUsArg = bundle.getParcelableArrayList(ARGUMENT.QUANTITY_UNITS);
    assert qUsArg != null;
    quantityUnits = new ArrayList<>(qUsArg);

    SortUtil.sortQuantityUnitsByName(requireContext(), quantityUnits, true);
    if (bundle.getBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, false)) {
      quantityUnits.add(
          0,
          new QuantityUnit(-1, getString(R.string.subtitle_none_selected)));
    }
    int selected = bundle.getInt(Constants.ARGUMENT.SELECTED_ID, -1);

    TextView textViewTitle = view.findViewById(R.id.text_list_selection_title);
    textViewTitle.setText(activity.getString(R.string.property_quantity_units));

    RecyclerView recyclerView = view.findViewById(R.id.recycler_list_selection);
    recyclerView.setLayoutManager(
        new LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )
    );
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(
        new QuantityUnitAdapter(
            quantityUnits, selected, this
        )
    );

    return view;
  }

  @Override
  public void onItemRowClicked(int position) {
    activity.getCurrentFragment().selectQuantityUnit(quantityUnits.get(position));
    activity.getCurrentFragment().selectQuantityUnit(
        quantityUnits.get(position), requireArguments()
    );
    dismiss();
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    activity.getCurrentFragment().onBottomSheetDismissed();
    super.onDismiss(dialog);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}