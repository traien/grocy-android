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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
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
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.StoreAdapter;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class StoresBottomSheet extends BaseBottomSheet
    implements StoreAdapter.StoreAdapterListener {

  private final static String TAG = StoresBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private ArrayList<Store> stores;

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

    ArrayList<Store> storesArg = bundle.getParcelableArrayList(Constants.ARGUMENT.STORES);
    assert storesArg != null;
    stores = new ArrayList<>(storesArg);

    SortUtil.sortStoresByName(requireContext(), stores, true);
    if (bundle.getBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, false)) {
      stores.add(0, new Store(-1, getString(R.string.subtitle_none_selected)));
    }
    int selected = bundle.getInt(Constants.ARGUMENT.SELECTED_ID, -1);

    TextView textViewTitle = view.findViewById(R.id.text_list_selection_title);
    textViewTitle.setText(activity.getString(R.string.property_stores));

    RecyclerView recyclerView = view.findViewById(R.id.recycler_list_selection);
    recyclerView.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(new StoreAdapter(stores, selected, this));
    return view;
  }

  @Override
  public void onItemRowClicked(int position) {
    BaseFragment currentFragment = activity.getCurrentFragment();
    currentFragment.selectStore(stores.get(position));
    dismiss();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
