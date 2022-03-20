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

package de.osamaco.grocy.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import de.osamaco.grocy.dao.LocationDao;
import de.osamaco.grocy.dao.MissingItemDao;
import de.osamaco.grocy.dao.PendingProductBarcodeDao;
import de.osamaco.grocy.dao.PendingProductDao;
import de.osamaco.grocy.dao.PendingPurchaseDao;
import de.osamaco.grocy.dao.ProductAveragePriceDao;
import de.osamaco.grocy.dao.ProductBarcodeDao;
import de.osamaco.grocy.dao.ProductDao;
import de.osamaco.grocy.dao.ProductGroupDao;
import de.osamaco.grocy.dao.ProductLastPurchasedDao;
import de.osamaco.grocy.dao.QuantityUnitConversionDao;
import de.osamaco.grocy.dao.QuantityUnitDao;
import de.osamaco.grocy.dao.ShoppingListDao;
import de.osamaco.grocy.dao.ShoppingListItemDao;
import de.osamaco.grocy.dao.StockItemDao;
import de.osamaco.grocy.dao.StockLocationDao;
import de.osamaco.grocy.dao.StoreDao;
import de.osamaco.grocy.dao.TaskCategoryDao;
import de.osamaco.grocy.dao.TaskDao;
import de.osamaco.grocy.model.Location;
import de.osamaco.grocy.model.MissingItem;
import de.osamaco.grocy.model.PendingProduct;
import de.osamaco.grocy.model.PendingProductBarcode;
import de.osamaco.grocy.model.PendingPurchase;
import de.osamaco.grocy.model.Product;
import de.osamaco.grocy.model.ProductAveragePrice;
import de.osamaco.grocy.model.ProductBarcode;
import de.osamaco.grocy.model.ProductGroup;
import de.osamaco.grocy.model.ProductLastPurchased;
import de.osamaco.grocy.model.QuantityUnit;
import de.osamaco.grocy.model.QuantityUnitConversion;
import de.osamaco.grocy.model.ShoppingList;
import de.osamaco.grocy.model.ShoppingListItem;
import de.osamaco.grocy.model.StockItem;
import de.osamaco.grocy.model.StockLocation;
import de.osamaco.grocy.model.Store;
import de.osamaco.grocy.model.Task;
import de.osamaco.grocy.model.TaskCategory;

@Database(
    entities = {
        ShoppingList.class,
        ShoppingListItem.class,
        Product.class,
        ProductGroup.class,
        QuantityUnit.class,
        Store.class,
        Location.class,
        MissingItem.class,
        QuantityUnitConversion.class,
        ProductBarcode.class,
        StockItem.class,
        StockLocation.class,
        Task.class,
        TaskCategory.class,
        ProductLastPurchased.class,
        ProductAveragePrice.class,
        PendingProduct.class,
        PendingProductBarcode.class,
        PendingPurchase.class
    },
    version = 27
)
public abstract class AppDatabase extends RoomDatabase {

  private static AppDatabase INSTANCE;

  public abstract ShoppingListDao shoppingListDao();

  public abstract ShoppingListItemDao shoppingListItemDao();

  public abstract ProductDao productDao();

  public abstract ProductGroupDao productGroupDao();

  public abstract QuantityUnitDao quantityUnitDao();

  public abstract StoreDao storeDao();

  public abstract LocationDao locationDao();

  public abstract MissingItemDao missingItemDao();

  public abstract QuantityUnitConversionDao quantityUnitConversionDao();

  public abstract ProductBarcodeDao productBarcodeDao();

  public abstract StockItemDao stockItemDao();

  public abstract StockLocationDao stockLocationDao();

  public abstract TaskDao taskDao();

  public abstract TaskCategoryDao taskCategoryDao();

  public abstract ProductLastPurchasedDao productLastPurchasedDao();

  public abstract ProductAveragePriceDao productAveragePriceDao();

  public abstract PendingProductDao pendingProductDao();

  public abstract PendingProductBarcodeDao pendingProductBarcodeDao();

  public abstract PendingPurchaseDao pendingPurchaseDao();

  public static AppDatabase getAppDatabase(Context context) {
    if (INSTANCE == null) {
      INSTANCE = Room.databaseBuilder(
          context.getApplicationContext(),
          AppDatabase.class,
          "app_database"
      ).fallbackToDestructiveMigration().build();
    }
    return INSTANCE;
  }

  public static void destroyInstance() {
    INSTANCE = null;
  }
}
