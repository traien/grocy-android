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
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "shopping_list_table")
public class ShoppingList implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private final int id;

  @ColumnInfo(name = "name")
  @SerializedName("name")
  private final String name;

  @ColumnInfo(name = "notes")
  @SerializedName("description")
  private String notes;

  public ShoppingList(int id, String name, String notes) {
    this.id = id;
    this.name = name;
    this.notes = notes;
  }

  private ShoppingList(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    notes = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(notes);
  }

  public static final Creator<ShoppingList> CREATOR = new Creator<ShoppingList>() {

    @Override
    public ShoppingList createFromParcel(Parcel in) {
      return new ShoppingList(in);
    }

    @Override
    public ShoppingList[] newArray(int size) {
      return new ShoppingList[size];
    }
  };

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @NonNull
  @Override
  public String toString() {
    return "ShoppingListEntity{id=" + id + ", name='" + name + "', notes='" + notes + "'}";
  }
}
