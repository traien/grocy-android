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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentLoginApiFormBinding;
import de.osamaco.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import de.osamaco.grocy.util.ClickUtil;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.NetUtil;
import de.osamaco.grocy.viewmodel.LoginApiFormViewModel;

public class LoginApiFormFragment extends BaseFragment {

  private FragmentLoginApiFormBinding binding;
  private MainActivity activity;
  private LoginApiFormViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentLoginApiFormBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    LoginApiFormFragmentArgs args = LoginApiFormFragmentArgs.fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new LoginApiFormViewModel
        .LoginApiFormViewModelFactory(activity.getApplication(), args)
    ).get(LoginApiFormViewModel.class);
    binding.setFragment(this);
    binding.setClickUtil(new ClickUtil());
    binding.setViewModel(viewModel);
    binding.setFormData(viewModel.getFormData());
    binding.setLifecycleOwner(getViewLifecycleOwner());
    binding.setActivity(activity);
  }

  public void proceedWithLogin() {
    if (!viewModel.getFormData().isFormValid()) {
      binding.serverUrl.clearFocus();
      binding.token.clearFocus();
      binding.ingressId.clearFocus();
      binding.apiKey.clearFocus();
      activity.hideKeyboard();
      return;
    }
    String ingressProxyId = viewModel.getFormData().getIngressProxyIdTrimmed();
    String hassServerUrl = viewModel.getFormData().getServerUrlTrimmed();
    String grocyServerUrl = viewModel.getFormData().getServerUrlTrimmed();
    if (ingressProxyId != null) {
      grocyServerUrl += "/api/hassio_ingress/" + ingressProxyId;
    }
    navigate(LoginApiFormFragmentDirections.actionLoginApiFormFragmentToLoginRequestFragment(
        grocyServerUrl,
        viewModel.getFormData().getApiKeyTrimmed()
    ).setHomeAssistantServerUrl(
        ingressProxyId != null ? hassServerUrl : null
    ).setHomeAssistantToken(
        ingressProxyId != null ? viewModel.getFormData().getLongLivedAccessTokenTrimmed()
            : null
    ));
  }

  public void openHomeAssistantProfileWebsite() {
    if (!viewModel.getFormData().isServerUrlValid()) {
      return;
    }
    Intent browserManageKeys = new Intent(Intent.ACTION_VIEW);
    String url = viewModel.getFormData().getServerUrlTrimmed();
    url += "/profile";
    Uri uri = Uri.parse(url);
    browserManageKeys.setData(uri);
    startActivity(browserManageKeys);
  }

  public void openApiKeysWebsite() {
    if (!viewModel.getFormData().isServerUrlValid()) {
      return;
    }
    Intent browserManageKeys = new Intent(Intent.ACTION_VIEW);
    String url = viewModel.getFormData().getServerUrlTrimmed();
    if (!viewModel.getFormData().getUsingGrocyHassAddOn()) {
      url += "/manageapikeys";
    }
    Uri uri = Uri.parse(url);
    browserManageKeys.setData(uri);
    startActivity(browserManageKeys);
  }

  public void openNabuCasaWebsite() {
    Uri uri = Uri.parse("https://www.nabucasa.com/");
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(uri);
    startActivity(intent);
  }

  public void openHelpWebsite() {
    boolean success = NetUtil.openURL(requireContext(), Constants.URL.HELP);
    if (!success) {
      activity.showMessage(R.string.error_no_browser);
    }
  }

  public void openGrocyWebsite() {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_grocy))));
  }

  public void showFeedbackBottomSheet() {
    activity.showBottomSheet(new FeedbackBottomSheet());
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.background);
  }
}
