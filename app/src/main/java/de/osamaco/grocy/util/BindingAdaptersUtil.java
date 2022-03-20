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

package de.osamaco.grocy.util;

import android.animation.LayoutTransition;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.MutableLiveData;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;
import de.osamaco.grocy.adapter.MatchProductsArrayAdapter;
import de.osamaco.grocy.model.Product;
import de.osamaco.grocy.view.ActionButton;

public class BindingAdaptersUtil {

  @BindingAdapter({"errorText"})
  public static void setErrorMessage(TextInputLayout view, MutableLiveData<Integer> errorMsg) {
    if (errorMsg.getValue() != null) {
      view.setError(view.getContext().getString(errorMsg.getValue()));
    } else if (view.isErrorEnabled()) {
      view.setErrorEnabled(false);
    }
  }

  @BindingAdapter({"errorText"})
  public static void setErrorMessageStr(TextInputLayout view, MutableLiveData<String> errorMsg) {
    if (errorMsg.getValue() != null) {
      view.setError(errorMsg.getValue());
    } else if (view.isErrorEnabled()) {
      view.setErrorEnabled(false);
    }
  }

  @BindingAdapter({"progressBackgroundColor"})
  public static void setProgressBackgroundColor(SwipeRefreshLayout view, @ColorInt int color) {
    view.setProgressBackgroundColorSchemeColor(color);
  }

  @BindingAdapter({"progressForegroundColor"})
  public static void setColorSchemeColors(SwipeRefreshLayout view, @ColorInt int color) {
    view.setColorSchemeColors(color);
  }

  @BindingAdapter({"transitionTypeChanging"})
  public static void setTransitionTypeChanging(ViewGroup view, boolean enabled) {
    if (!enabled) {
      return;
    }
    view.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
  }

  @BindingAdapter({"productList"})
  public static void setProductList(MaterialAutoCompleteTextView view, List<Product> items) {
    if (items == null) {
      return;
    }
    view.setAdapter(
        new MatchProductsArrayAdapter(view.getContext(), android.R.layout.simple_list_item_1,
            items));
  }

  @BindingAdapter("onSearchClickInSoftKeyboard")
  public static void setOnSearchClickInSoftKeyboardListener(
      EditText view,
      Runnable listener
  ) {
    view.setOnEditorActionListener(listener == null ? null : (v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        listener.run();
        return true;
      }
      return false;
    });
  }

  @BindingAdapter("onDoneClickInSoftKeyboard")
  public static void setOnDoneClickInSoftKeyboardListener(
      EditText view,
      Runnable listener
  ) {
    view.setOnEditorActionListener(listener == null ? null : (v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        listener.run();
        return true;
      }
      return false;
    });
  }

  @BindingAdapter("onNextClickInSoftKeyboard")
  public static void setOnNextClickInSoftKeyboardListener(
      EditText view,
      Runnable listener
  ) {
    view.setOnEditorActionListener(listener == null ? null : (v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_NEXT) {
        listener.run();
        return true;
      }
      return false;
    });
  }

  @BindingAdapter("endIconOnLongClickListenerCustom")
  public static void setEndIconOnLongClickListenerCustom(TextInputLayout view, Runnable listener) {
    view.setEndIconOnLongClickListener(v -> {
      listener.run();
      return true;
    });
  }

  @BindingAdapter(value = {"android:onClick", "clickUtil", "iconToAnimate"}, requireAll = false)
  public static void setOnClickListener(
      View view,
      View.OnClickListener listener,
      ClickUtil clickUtil,
      View iconToAnimate
  ) {
    if (view == null) {
      return;
    }
    view.setOnClickListener(v -> {
      if (clickUtil != null && clickUtil.isDisabled()) {
        return;
      }
      if (iconToAnimate instanceof ActionButton && ((ActionButton) view).hasAnimatable()) {
        ((ActionButton) iconToAnimate).startIconAnimation();
      } else if (view instanceof ActionButton && ((ActionButton) view).hasAnimatable()) {
        ((ActionButton) view).startIconAnimation();
      } else if (iconToAnimate instanceof ImageView) {
        ViewUtil.startIcon(iconToAnimate);
      }
      if (listener != null) {
        listener.onClick(view);
      }
    });
  }

  @BindingAdapter("android:onLongClickVoid")
  public static void setOnLongClickListener(View view, Runnable listener) {
    view.setOnLongClickListener(v -> {
      listener.run();
      return true;
    });
  }

  @BindingAdapter(value = {"android:onCheckedChanged", "iconToAnimate", "initialChecked"}, requireAll = false)
  public static void setOnCheckedChangedListener(SwitchMaterial view, CompoundButton.OnCheckedChangeListener listener, View iconToAnimate, Boolean initialChecked) {
    if (initialChecked != null) {
      view.setChecked(initialChecked);
    }
    view.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (iconToAnimate instanceof ImageView) {
        ViewUtil.startIcon(iconToAnimate);
      }
      if (listener != null) {
        listener.onCheckedChanged(view, isChecked);
      }
    });
  }
}
