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

import androidx.annotation.Nullable;
import de.osamaco.grocy.api.GrocyApi;
import de.osamaco.grocy.api.GrocyApi.ENTITY;
import de.osamaco.grocy.model.Location;
import de.osamaco.grocy.model.Product;
import de.osamaco.grocy.model.ProductGroup;
import de.osamaco.grocy.model.QuantityUnit;
import de.osamaco.grocy.model.Store;
import de.osamaco.grocy.model.TaskCategory;

public class ObjectUtil {

  public static int getObjectId(Object object, String entity) {
    switch (entity) {
      case GrocyApi.ENTITY.QUANTITY_UNITS:
        return ((QuantityUnit) object).getId();
      case GrocyApi.ENTITY.LOCATIONS:
        return ((Location) object).getId();
      case GrocyApi.ENTITY.PRODUCT_GROUPS:
        return ((ProductGroup) object).getId();
      case GrocyApi.ENTITY.STORES:
        return ((Store) object).getId();
      case GrocyApi.ENTITY.PRODUCTS:
        return ((Product) object).getId();
      default:
        return -1;
    }
  }

  @Nullable
  public static String getObjectName(Object object, String entity) {
    switch (entity) {
      case GrocyApi.ENTITY.QUANTITY_UNITS:
        return ((QuantityUnit) object).getName();
      case GrocyApi.ENTITY.LOCATIONS:
        return ((Location) object).getName();
      case GrocyApi.ENTITY.PRODUCT_GROUPS:
        return ((ProductGroup) object).getName();
      case GrocyApi.ENTITY.STORES:
        return ((Store) object).getName();
      case GrocyApi.ENTITY.PRODUCTS:
        return ((Product) object).getName();
      case ENTITY.TASK_CATEGORIES:
        return ((TaskCategory) object).getName();
      default:
        return null;
    }
  }

  @Nullable
  public static String getObjectDescription(Object object, String entity) {
    switch (entity) {
      case GrocyApi.ENTITY.QUANTITY_UNITS:
        return ((QuantityUnit) object).getDescription();
      case GrocyApi.ENTITY.LOCATIONS:
        return ((Location) object).getDescription();
      case GrocyApi.ENTITY.PRODUCT_GROUPS:
        return ((ProductGroup) object).getDescription();
      case GrocyApi.ENTITY.STORES:
        return ((Store) object).getDescription();
      case GrocyApi.ENTITY.PRODUCTS:
        return ((Product) object).getDescription();
      default:
        return null;
    }
  }
}
