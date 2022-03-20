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
import de.osamaco.grocy.model.PendingProduct;
import de.osamaco.grocy.model.Product;

public class ChooseProductRepository {

  private final AppDatabase appDatabase;

  public ChooseProductRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {
    void actionFinished(ChooseProductData data);
  }

  public static class ChooseProductData {

    private final List<Product> products;
    private final List<PendingProduct> pendingProducts;

    public ChooseProductData(
            List<Product> products,
            List<PendingProduct> pendingProducts
    ) {
      this.products = products;
      this.pendingProducts = pendingProducts;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<PendingProduct> getPendingProducts() {
      return pendingProducts;
    }
  }

  public void loadFromDatabase(DataListener listener) {
    Single.zip(
        appDatabase.productDao().getProducts(),
        appDatabase.pendingProductDao().getPendingProducts(),
        ChooseProductData::new
    )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }

  public void createPendingProduct(
          PendingProduct pendingProduct,
          CreatePendingProductListener successListener,
          Runnable errorListener
  ) {
    appDatabase.pendingProductDao().insertPendingProduct(pendingProduct)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess(successListener::onSuccess)
            .doOnError(throwable -> errorListener.run())
            .subscribe();
  }

  public interface CreatePendingProductListener {
    void onSuccess(long pendingProductId);
  }
}
