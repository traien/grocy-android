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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.adapter.ShoppingListAdapter;
import de.osamaco.grocy.adapter.ShoppingPlaceholderAdapter;
import de.osamaco.grocy.fragment.ShoppingListFragment;
import de.osamaco.grocy.fragment.ShoppingListFragmentDirections;
import de.osamaco.grocy.model.ShoppingList;
import de.osamaco.grocy.repository.ShoppingListRepository;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.view.ActionButton;

public class ShoppingListsBottomSheet extends BaseBottomSheet
    implements ShoppingListAdapter.ShoppingListAdapterListener {

  private final static int DELETE_CONFIRMATION_DURATION = 2000;
  private final static String TAG = ShoppingListsBottomSheet.class.getSimpleName();

  private MainActivity activity;

  private ProgressBar progressConfirm;
  private ValueAnimator confirmProgressAnimator;

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

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    boolean multipleListsFeature = sharedPrefs.getBoolean(
        Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true
    );

    MutableLiveData<Integer> selectedIdLive = activity.getCurrentFragment()
        .getSelectedShoppingListIdLive();
    if (selectedIdLive == null) {
      dismiss();
      return view;
    }

    ShoppingListRepository repository = new ShoppingListRepository(activity.getApplication());

    TextView textViewTitle = view.findViewById(R.id.text_list_selection_title);
    textViewTitle.setText(activity.getString(R.string.property_shopping_lists));

    RecyclerView recyclerView = view.findViewById(R.id.recycler_list_selection);
    recyclerView.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(new ShoppingPlaceholderAdapter());

    repository.getShoppingListsLive().observe(getViewLifecycleOwner(), shoppingLists -> {
      if (shoppingLists == null) {
        return;
      }
      if (recyclerView.getAdapter() == null
          || !(recyclerView.getAdapter() instanceof ShoppingListAdapter)
      ) {
        recyclerView.setAdapter(new ShoppingListAdapter(
            shoppingLists,
            selectedIdLive.getValue(),
            this,
            activity.getCurrentFragment() instanceof ShoppingListFragment
                && activity.isOnline()
        ));
      } else {
        ((ShoppingListAdapter) recyclerView.getAdapter()).updateData(
            shoppingLists,
            selectedIdLive.getValue()
        );
      }
    });

    selectedIdLive.observe(getViewLifecycleOwner(), selectedId -> {
      if (recyclerView.getAdapter() == null
          || !(recyclerView.getAdapter() instanceof ShoppingListAdapter)
      ) {
        return;
      }
      ((ShoppingListAdapter) recyclerView.getAdapter()).updateSelectedId(
          selectedIdLive.getValue()
      );
    });

    ActionButton buttonNew = view.findViewById(R.id.button_list_selection_new);
    if (activity.isOnline() && multipleListsFeature
        && activity.getCurrentFragment() instanceof ShoppingListFragment
    ) {
      buttonNew.setVisibility(View.VISIBLE);
      buttonNew.setOnClickListener(v -> {
        dismiss();
        navigate(ShoppingListFragmentDirections
            .actionShoppingListFragmentToShoppingListEditFragment());
      });
    }

    progressConfirm = view.findViewById(R.id.progress_confirmation);

    return view;
  }

  @Override
  public void onDestroyView() {
    if (confirmProgressAnimator != null) {
      confirmProgressAnimator.cancel();
      confirmProgressAnimator = null;
    }
    super.onDestroyView();
  }

  @Override
  public void onItemRowClicked(ShoppingList shoppingList) {
    activity.getCurrentFragment().selectShoppingList(shoppingList);
    dismiss();
  }

  @Override
  public void onClickEdit(ShoppingList shoppingList) {
    if (!activity.isOnline()) {
      showMessage(R.string.error_offline);
      return;
    }
    dismiss();
    navigate(ShoppingListFragmentDirections
        .actionShoppingListFragmentToShoppingListEditFragment()
        .setShoppingList(shoppingList));
  }

  @Override
  public void onTouchDelete(View view, MotionEvent event, ShoppingList shoppingList) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (!activity.isOnline()) {
        showMessage(R.string.error_offline);
        return;
      }
      showAndStartProgress(view, shoppingList);
    } else if (event.getAction() == MotionEvent.ACTION_UP
        || event.getAction() == MotionEvent.ACTION_CANCEL) {
      if (!activity.isOnline()) {
        return;
      }
      hideAndStopProgress();
    }
  }

  private void showAndStartProgress(View buttonView, ShoppingList shoppingList) {
    assert getView() != null;
    TransitionManager.beginDelayedTransition((ViewGroup) getView());
    progressConfirm.setVisibility(View.VISIBLE);
    int startValue = 0;
    if (confirmProgressAnimator != null) {
      startValue = progressConfirm.getProgress();
      if (startValue == 100) {
        startValue = 0;
      }
      confirmProgressAnimator.removeAllListeners();
      confirmProgressAnimator.cancel();
      confirmProgressAnimator = null;
    }
    confirmProgressAnimator = ValueAnimator.ofInt(startValue, progressConfirm.getMax());
    confirmProgressAnimator.setDuration(DELETE_CONFIRMATION_DURATION
        * (progressConfirm.getMax() - startValue) / progressConfirm.getMax());
    confirmProgressAnimator.addUpdateListener(
        animation -> progressConfirm.setProgress((Integer) animation.getAnimatedValue())
    );
    confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        int currentProgress = progressConfirm.getProgress();
        if (currentProgress == progressConfirm.getMax()) {
          TransitionManager.beginDelayedTransition((ViewGroup) requireView());
          progressConfirm.setVisibility(View.GONE);
          ImageView buttonImage = buttonView.findViewById(R.id.image_action_button);
          ((Animatable) buttonImage.getDrawable()).start();
          activity.getCurrentFragment().deleteShoppingList(shoppingList);
          return;
        }
        confirmProgressAnimator = ValueAnimator.ofInt(currentProgress, 0);
        confirmProgressAnimator.setDuration((DELETE_CONFIRMATION_DURATION / 2)
            * currentProgress / progressConfirm.getMax());
        confirmProgressAnimator.setInterpolator(new FastOutSlowInInterpolator());
        confirmProgressAnimator.addUpdateListener(
            anim -> progressConfirm.setProgress((Integer) anim.getAnimatedValue())
        );
        confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            TransitionManager.beginDelayedTransition((ViewGroup) requireView());
            progressConfirm.setVisibility(View.GONE);
          }
        });
        confirmProgressAnimator.start();
      }
    });
    confirmProgressAnimator.start();
  }

  private void hideAndStopProgress() {
    if (confirmProgressAnimator != null) {
      confirmProgressAnimator.cancel();
    }

    if (progressConfirm.getProgress() != 100) {
      showMessage(R.string.msg_press_hold_confirm);
    }
  }

  private void showMessage(@StringRes int message) {
    assert getView() != null;
    Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
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
