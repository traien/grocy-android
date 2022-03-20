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

package de.osamaco.grocy.scanner;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.ColorRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import de.osamaco.grocy.R;
import de.osamaco.grocy.util.Constants.SETTINGS.SCANNER;
import de.osamaco.grocy.util.Constants.SETTINGS_DEFAULT;

public class EmbeddedFragmentScannerBundle extends EmbeddedFragmentScanner {

  private final static String TAG = EmbeddedFragmentScannerBundle.class.getSimpleName();
  private final EmbeddedFragmentScanner embeddedFragmentScanner;

  public EmbeddedFragmentScannerBundle(
      Fragment fragment,
      CoordinatorLayout containerScanner,
      BarcodeListener barcodeListener,
      @ColorRes int viewfinderMaskColorZXing,
      boolean qrCodeFormat,
      boolean takeSmallQrCodeFormat
  ) {
    super(fragment.requireActivity());
    embeddedFragmentScanner = new EmbeddedFragmentScannerZXing(
        fragment,
        containerScanner,
        barcodeListener,
        viewfinderMaskColorZXing,
        qrCodeFormat,
        takeSmallQrCodeFormat
    );
  }

  public EmbeddedFragmentScannerBundle(
      Fragment fragment,
      CoordinatorLayout containerScanner,
      BarcodeListener barcodeListener,
      @ColorRes int viewfinderMaskColorZXing
  ) {
    this(
        fragment,
        containerScanner,
        barcodeListener,
        viewfinderMaskColorZXing,
        useScannerFormat2d(fragment),
        true
    );
  }

  public EmbeddedFragmentScannerBundle(
      Fragment fragment,
      CoordinatorLayout containerScanner,
      BarcodeListener barcodeListener
  ) {
    this(
        fragment,
        containerScanner,
        barcodeListener,
        R.color.background,
        useScannerFormat2d(fragment),
        true
    );
  }

  private static boolean useScannerFormat2d(Fragment fragment) {
    SharedPreferences sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(fragment.requireContext());
    return sharedPreferences.getBoolean(
        SCANNER.SCANNER_FORMAT_2D,
        SETTINGS_DEFAULT.SCANNER.SCANNER_FORMAT_2D
    );
  }

  @Override
  public void setScannerVisibilityLive(LiveData<Boolean> scannerVisibilityLive) {
    embeddedFragmentScanner.setScannerVisibilityLive(scannerVisibilityLive);
  }

  @Override
  public void setScannerVisibilityLive(
      LiveData<Boolean> scannerVisibilityLive,
      boolean supressNextScanStart
  ) {
    embeddedFragmentScanner.setScannerVisibilityLive(scannerVisibilityLive, supressNextScanStart);
  }

  @Override
  public void onResume() {
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
  }

  @Override
  public void onDestroy() {
    embeddedFragmentScanner.onDestroy();
  }

  @Override
  void stopScanner() {
    embeddedFragmentScanner.stopScanner();
  }

  @Override
  public void startScannerIfVisible() {
    embeddedFragmentScanner.startScannerIfVisible();
  }

  @Override
  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }
}
