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

package de.osamaco.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

public class StockEntry implements Parcelable {

  @SerializedName("id")
  private int id;

  @SerializedName("product_id")
  private int productId;

  @SerializedName("amount")
  private double amount;

  @SerializedName("best_before_date")
  private String bestBeforeDate;

  @SerializedName("purchased_date")
  private String purchasedDate;

  @SerializedName("stock_id")
  private final String stockId;

  @SerializedName("price")
  private String price;

  @SerializedName("open")
  private int open;

  @SerializedName("opened_date")
  private String openedDate;

  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  @SerializedName("location_id")
  private int locationId;

  public StockEntry() {
    stockId = null;
  }

  public StockEntry(int id, String stockId) {
    this.id = id;
    this.stockId = stockId;
  }

  private StockEntry(Parcel parcel) {
    id = parcel.readInt();
    productId = parcel.readInt();
    amount = parcel.readDouble();
    bestBeforeDate = parcel.readString();
    purchasedDate = parcel.readString();
    stockId = parcel.readString();
    price = parcel.readString();
    open = parcel.readInt();
    openedDate = parcel.readString();
    rowCreatedTimestamp = parcel.readString();
    locationId = parcel.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(productId);
    dest.writeDouble(amount);
    dest.writeString(bestBeforeDate);
    dest.writeString(purchasedDate);
    dest.writeString(stockId);
    dest.writeString(price);
    dest.writeInt(open);
    dest.writeString(openedDate);
    dest.writeString(rowCreatedTimestamp);
    dest.writeInt(locationId);
  }

  public static final Creator<StockEntry> CREATOR = new Creator<StockEntry>() {

    @Override
    public StockEntry createFromParcel(Parcel in) {
      return new StockEntry(in);
    }

    @Override
    public StockEntry[] newArray(int size) {
      return new StockEntry[size];
    }
  };

  public int getId() {
    return id;
  }

  public int getProductId() {
    return productId;
  }

  public double getAmount() {
    return amount;
  }

  public String getBestBeforeDate() {
    return bestBeforeDate;
  }

  public String getPurchasedDate() {
    return purchasedDate;
  }

  public String getStockId() {
    return stockId;
  }

  public String getPrice() {
    return price;
  }

  public int getOpen() {
    return open;
  }

  public String getOpenedDate() {
    return openedDate;
  }

  public String getRowCreatedTimestamp() {
    return rowCreatedTimestamp;
  }

  public int getLocationId() {
    return locationId;
  }

  public static StockEntry getStockEntryFromId(ArrayList<StockEntry> stockEntries, String id) {
    for (StockEntry stockEntry : stockEntries) {
      if (stockEntry.getStockId().equals(id)) {
        return stockEntry;
      }
    }
    return null;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @NonNull
  @Override
  public String toString() {
    return "StockEntry(" + productId + ")";
  }
}
