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
import androidx.lifecycle.MutableLiveData;
import de.osamaco.grocy.R;
import de.osamaco.grocy.activity.MainActivity;
import de.osamaco.grocy.databinding.FragmentLoginApiQrCodeBinding;
import de.osamaco.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import de.osamaco.grocy.scanner.EmbeddedFragmentScanner;
import de.osamaco.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import de.osamaco.grocy.scanner.EmbeddedFragmentScannerBundle;
import de.osamaco.grocy.util.ClickUtil;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.NetUtil;

public class LoginApiQrCodeFragment extends BaseFragment implements BarcodeListener {

  private static final int SCAN_GROCY_KEY = 0;
  private static final int SCAN_HASS_TOKEN = 1;

  private FragmentLoginApiQrCodeBinding binding;
  private MainActivity activity;
  private LoginApiQrCodeFragmentArgs args;
  private EmbeddedFragmentScanner embeddedFragmentScanner;
  private int pageStatus = SCAN_GROCY_KEY;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentLoginApiQrCodeBinding.inflate(inflater, container, false);
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
        this,
        binding.containerScanner,
        this,
        R.color.background,
        true,
        false
    );
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
    args = LoginApiQrCodeFragmentArgs.fromBundle(requireArguments());
    pageStatus = args.getGrocyApiKey() == null ? SCAN_GROCY_KEY : SCAN_HASS_TOKEN;

    binding.setFragment(this);
    binding.setClickUtil(new ClickUtil());
    embeddedFragmentScanner.setScannerVisibilityLive(new MutableLiveData<>(true));
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
    if(pageStatus == SCAN_GROCY_KEY) {
      String[] resultSplit = rawValue.split("\\|");
      if (resultSplit.length != 2) {
        activity.showMessage(R.string.error_api_qr_code);
        embeddedFragmentScanner.startScannerIfVisible();
        return;
      }
      String apiURL = resultSplit[0];
      String serverURL = apiURL.replaceAll("/api$", "");
      String ingressProxyId = null;
      if (serverURL.startsWith("/api/hassio_ingress/")) {
        ingressProxyId = serverURL.replace("/api/hassio_ingress/", "");
      }
      String apiKey = resultSplit[1];

      if (ingressProxyId == null) {
        navigate(LoginApiQrCodeFragmentDirections
            .actionLoginApiQrCodeFragmentToLoginRequestFragment(serverURL, apiKey));
      } else { // grocy home assistant add-on used
        navigate(LoginApiQrCodeFragmentDirections
            .actionLoginApiQrCodeFragmentSelf()
            .setGrocyIngressProxyId(ingressProxyId)
            .setGrocyApiKey(apiKey));
      }
    } else if (pageStatus == SCAN_HASS_TOKEN) {
      String[] resultSplit = rawValue.split("\\.");
      if (resultSplit.length != 3) {
        activity.showMessage(R.string.error_token_qr_code);
        embeddedFragmentScanner.startScannerIfVisible();
        return;
      }
      navigate(LoginApiQrCodeFragmentDirections
          .actionLoginApiQrCodeFragmentToLoginApiFormFragment()
          .setGrocyIngressProxyId(args.getGrocyIngressProxyId())
          .setGrocyApiKey(args.getGrocyApiKey())
          .setHomeAssistantToken(rawValue));
    }
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }

  public void enterDataManually() {
    navigate(LoginApiQrCodeFragmentDirections
        .actionLoginApiQrCodeFragmentToLoginApiFormFragment());
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

  public boolean isPageForGrocyKey() {
    return pageStatus == SCAN_GROCY_KEY;
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.background);
  }
}
