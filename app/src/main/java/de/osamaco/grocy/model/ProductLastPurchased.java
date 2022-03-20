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
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@Entity(tableName = "product_last_purchased_table")
public class ProductLastPurchased implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private int productId;

  @ColumnInfo(name = "amount")
  @SerializedName("amount")
  private String amount;

  @ColumnInfo(name = "best_before_date")
  @SerializedName("best_before_date")
  private String bestBeforeDate;

  @ColumnInfo(name = "purchased_date")
  @SerializedName("purchased_date")
  private String purchasedDate;

  @ColumnInfo(name = "price")
  @SerializedName("price")
  private String price;

  @ColumnInfo(name = "location_id")
  @SerializedName("location_id")
  private String locationId;

  @ColumnInfo(name = "shopping_location_id")
  @SerializedName("shopping_location_id")
  private String shoppingLocationId;

  public ProductLastPurchased() {
  }

  @Ignore
  public ProductLastPurchased(Parcel parcel) {
    productId = parcel.readInt();
    amount = parcel.readString();
    bestBeforeDate = parcel.readString();
    purchasedDate = parcel.readString();
    price = parcel.readString();
    locationId = parcel.readString();
    shoppingLocationId = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(productId);
    dest.writeString(amount);
    dest.writeString(bestBeforeDate);
    dest.writeString(purchasedDate);
    dest.writeString(price);
    dest.writeString(locationId);
    dest.writeString(shoppingLocationId);
  }

  public static final Creator<ProductLastPurchased> CREATOR = new Creator<ProductLastPurchased>() {

    @Override
    public ProductLastPurchased createFromParcel(Parcel in) {
      return new ProductLastPurchased(in);
    }

    @Override
    public ProductLastPurchased[] newArray(int size) {
      return new ProductLastPurchased[size];
    }
  };

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public String getBestBeforeDate() {
    return bestBeforeDate;
  }

  public void setBestBeforeDate(String bestBeforeDate) {
    this.bestBeforeDate = bestBeforeDate;
  }

  public String getPurchasedDate() {
    return purchasedDate;
  }

  public void setPurchasedDate(String purchasedDate) {
    this.purchasedDate = purchasedDate;
  }

  public String getPrice() {
    return price;
  }

  public void setPrice(String price) {
    this.price = price;
  }

  public String getLocationId() {
    return locationId;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
  }

  public String getShoppingLocationId() {
    return shoppingLocationId;
  }

  public void setShoppingLocationId(String shoppingLocationId) {
    this.shoppingLocationId = shoppingLocationId;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProductLastPurchased that = (ProductLastPurchased) o;
    return productId == that.productId && Objects.equals(amount, that.amount) && Objects.equals(bestBeforeDate, that.bestBeforeDate) && Objects.equals(purchasedDate, that.purchasedDate) && Objects.equals(price, that.price) && Objects.equals(locationId, that.locationId) && Objects.equals(shoppingLocationId, that.shoppingLocationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId, amount, bestBeforeDate, purchasedDate, price, locationId, shoppingLocationId);
  }

  @NonNull
  @Override
  public String toString() {
    return "ProductLastPurchased(" + productId + ')';
  }
}
