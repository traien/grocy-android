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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.adapter.StockEntryAdapter;
import de.osamaco.grocy.model.StockEntry;
import de.osamaco.grocy.util.Constants;

public class StockEntriesBottomSheet extends BaseBottomSheet
    implements StockEntryAdapter.StockEntryAdapterListener {

  private final static String TAG = StockEntriesBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private ArrayList<StockEntry> stockEntries;

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
        R.layout.fragment_bottomsheet_stock_entries, container, false
    );

    activity = (MainActivity) requireActivity();
    Bundle bundle = requireArguments();

    stockEntries = bundle.getParcelableArrayList(Constants.ARGUMENT.STOCK_ENTRIES);
    String selectedStockId = bundle.getString(Constants.ARGUMENT.SELECTED_ID);

    // Add entry for automatic selection
    stockEntries.add(0, new StockEntry(-1, null));

    RecyclerView recyclerView = view.findViewById(R.id.recycler_stock_entries);
    recyclerView.setLayoutManager(
        new LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )
    );
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(
        new StockEntryAdapter(activity, stockEntries, selectedStockId, this)
    );

    return view;
  }

  @Override
  public void onItemRowClicked(int position) {
    StockEntry stockEntry = stockEntries.get(position);
    activity.getCurrentFragment().selectStockEntry(stockEntry.getId() != -1 ? stockEntry : null);
    dismiss();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
