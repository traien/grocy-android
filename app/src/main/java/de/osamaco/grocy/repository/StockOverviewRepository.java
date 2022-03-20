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

package de.osamaco.grocy.repository;

import android.app.Application;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import de.osamaco.grocy.database.AppDatabase;
import de.osamaco.grocy.model.Location;
import de.osamaco.grocy.model.Product;
import de.osamaco.grocy.model.ProductAveragePrice;
import de.osamaco.grocy.model.ProductBarcode;
import de.osamaco.grocy.model.ProductGroup;
import de.osamaco.grocy.model.ProductLastPurchased;
import de.osamaco.grocy.model.QuantityUnit;
import de.osamaco.grocy.model.ShoppingListItem;
import de.osamaco.grocy.model.StockItem;
import de.osamaco.grocy.model.StockLocation;
import de.osamaco.grocy.util.RxJavaUtil;

public class StockOverviewRepository {

  private final AppDatabase appDatabase;

  public StockOverviewRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface StockOverviewDataListener {
    void actionFinished(StockOverviewData data);
  }

  public static class StockOverviewData {

    private final List<QuantityUnit> quantityUnits;
    private final List<ProductGroup> productGroups;
    private final List<StockItem> stockItems;
    private final List<Product> products;
    private final List<ProductAveragePrice> productsAveragePrice;
    private final List<ProductLastPurchased> productsLastPurchased;
    private final List<ProductBarcode> productBarcodes;
    private final List<ShoppingListItem> shoppingListItems;
    private final List<Location> locations;
    private final List<StockLocation> stockCurrentLocations;

    public StockOverviewData(
        List<QuantityUnit> quantityUnits,
        List<ProductGroup> productGroups,
        List<StockItem> stockItems,
        List<Product> products,
        List<ProductAveragePrice> productsAveragePrice,
        List<ProductLastPurchased> productsLastPurchased,
        List<ProductBarcode> productBarcodes,
        List<ShoppingListItem> shoppingListItems,
        List<Location> locations,
        List<StockLocation> stockCurrentLocations
    ) {
      this.quantityUnits = quantityUnits;
      this.productGroups = productGroups;
      this.stockItems = stockItems;
      this.products = products;
      this.productsAveragePrice = productsAveragePrice;
      this.productsLastPurchased = productsLastPurchased;
      this.productBarcodes = productBarcodes;
      this.shoppingListItems = shoppingListItems;
      this.locations = locations;
      this.stockCurrentLocations = stockCurrentLocations;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<ProductGroup> getProductGroups() {
      return productGroups;
    }

    public List<StockItem> getStockItems() {
      return stockItems;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<ProductAveragePrice> getProductsAveragePrice() {
      return productsAveragePrice;
    }

    public List<ProductLastPurchased> getProductsLastPurchased() {
      return productsLastPurchased;
    }

    public List<ProductBarcode> getProductBarcodes() {
      return productBarcodes;
    }

    public List<ShoppingListItem> getShoppingListItems() {
      return shoppingListItems;
    }

    public List<Location> getLocations() {
      return locations;
    }

    public List<StockLocation> getStockCurrentLocations() {
      return stockCurrentLocations;
    }
  }

  public void loadFromDatabase(StockOverviewDataListener listener) {
    RxJavaUtil
        .zip(
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.productGroupDao().getProductGroups(),
            appDatabase.stockItemDao().getStockItems(),
            appDatabase.productDao().getProducts(),
            appDatabase.productAveragePriceDao().getProductsAveragePrice(),
            appDatabase.productLastPurchasedDao().getProductsLastPurchased(),
            appDatabase.productBarcodeDao().getProductBarcodes(),
            appDatabase.shoppingListItemDao().getShoppingListItems(),
            appDatabase.locationDao().getLocations(),
            appDatabase.stockLocationDao().getStockLocations(),
            StockOverviewData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }

  public void updateDatabase(
      List<StockItem> stockItems,
      Runnable listener
  ) {
    Single.concat(
        appDatabase.stockItemDao().deleteStockItems(),
        appDatabase.stockItemDao().insertStockItems(stockItems)
    )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally(listener::run)
        .subscribe();
  }
}
