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

package de.osamaco.grocy.helper;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import de.osamaco.grocy.R;
import de.osamaco.grocy.api.GrocyApi;
import de.osamaco.grocy.api.GrocyApi.ENTITY;
import de.osamaco.grocy.api.OpenBeautyFactsApi;
import de.osamaco.grocy.api.OpenFoodFactsApi;
import de.osamaco.grocy.database.AppDatabase;
import de.osamaco.grocy.model.Location;
import de.osamaco.grocy.model.MissingItem;
import de.osamaco.grocy.model.Product;
import de.osamaco.grocy.model.ProductAveragePrice;
import de.osamaco.grocy.model.ProductBarcode;
import de.osamaco.grocy.model.ProductDetails;
import de.osamaco.grocy.model.ProductGroup;
import de.osamaco.grocy.model.ProductLastPurchased;
import de.osamaco.grocy.model.QuantityUnit;
import de.osamaco.grocy.model.QuantityUnitConversion;
import de.osamaco.grocy.model.ShoppingList;
import de.osamaco.grocy.model.ShoppingListItem;
import de.osamaco.grocy.model.StockEntry;
import de.osamaco.grocy.model.StockItem;
import de.osamaco.grocy.model.StockLocation;
import de.osamaco.grocy.model.Store;
import de.osamaco.grocy.model.Task;
import de.osamaco.grocy.model.TaskCategory;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.Constants.PREF;
import de.osamaco.grocy.util.DateUtil;
import de.osamaco.grocy.util.PrefsUtil;
import de.osamaco.grocy.web.CustomJsonArrayRequest;
import de.osamaco.grocy.web.CustomJsonObjectRequest;
import de.osamaco.grocy.web.CustomStringRequest;
import de.osamaco.grocy.web.RequestQueueSingleton;

public class DownloadHelper {

  private final Application application;
  private final GrocyApi grocyApi;
  private final RequestQueue requestQueue;
  private final Gson gson;
  private final String uuidHelper;
  private final OnLoadingListener onLoadingListener;
  private final SharedPreferences sharedPrefs;
  private final DateUtil dateUtil;
  private final AppDatabase appDatabase;

  private final ArrayList<Queue> queueArrayList;
  private final String tag;
  private final String apiKey;
  private final String hassServerUrl;
  private final String hassLongLivedAccessToken;
  private final boolean debug;
  private final int timeoutSeconds;
  private int loadingRequests;

  public DownloadHelper(
      Application application,
      String tag,
      OnLoadingListener onLoadingListener
  ) {
    this.application = application;
    this.tag = tag;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    dateUtil = new DateUtil(application);
    appDatabase = AppDatabase.getAppDatabase(application.getApplicationContext());
    gson = new Gson();
    requestQueue = RequestQueueSingleton.getInstance(application).getRequestQueue();
    grocyApi = new GrocyApi(application);
    apiKey = sharedPrefs.getString(Constants.PREF.API_KEY, "");
    hassServerUrl = sharedPrefs.getString(
        Constants.PREF.HOME_ASSISTANT_SERVER_URL,
        null
    );
    hassLongLivedAccessToken = sharedPrefs.getString(
        Constants.PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN,
        null
    );
    uuidHelper = UUID.randomUUID().toString();
    queueArrayList = new ArrayList<>();
    loadingRequests = 0;
    this.onLoadingListener = onLoadingListener;
    timeoutSeconds = sharedPrefs.getInt(
        Constants.SETTINGS.NETWORK.LOADING_TIMEOUT,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT
    );
  }

  public DownloadHelper(
      Application application,
      String serverUrl,
      String apiKey,
      String hassServerUrl,
      String hassLongLivedAccessToken,
      String tag,
      OnLoadingListener onLoadingListener
  ) {
    this.application = application;
    this.tag = tag;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    gson = new Gson();
    dateUtil = new DateUtil(application);
    appDatabase = AppDatabase.getAppDatabase(application.getApplicationContext());
    RequestQueueSingleton.getInstance(application).newRequestQueue();
    requestQueue = RequestQueueSingleton.getInstance(application).getRequestQueue();
    grocyApi = new GrocyApi(application, serverUrl);
    this.apiKey = apiKey;
    this.hassServerUrl = hassServerUrl;
    this.hassLongLivedAccessToken = hassLongLivedAccessToken;
    uuidHelper = UUID.randomUUID().toString();
    queueArrayList = new ArrayList<>();
    loadingRequests = 0;
    this.onLoadingListener = onLoadingListener;
    timeoutSeconds = sharedPrefs.getInt(
        Constants.SETTINGS.NETWORK.LOADING_TIMEOUT,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT
    );
  }

  public DownloadHelper(Activity activity, String tag) {
    this(activity.getApplication(), tag, null);
  }

  // cancel all requests
  public void destroy() {
    for (Queue queue : queueArrayList) {
      queue.reset(true);
    }
    requestQueue.cancelAll(uuidHelper);
  }

  private void onRequestLoading() {
    loadingRequests += 1;
    if (onLoadingListener != null && loadingRequests == 1) {
      onLoadingListener.onLoadingChanged(true);
    }
  }

  private void onRequestFinished() {
    loadingRequests -= 1;
    if (onLoadingListener != null && loadingRequests == 0) {
      onLoadingListener.onLoadingChanged(false);
    }
  }

  public String getUuid() {
    return uuidHelper;
  }

  public void get(
      String url,
      String tag,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.GET,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          tag
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  // for requests without loading progress (set noLoadingProgress=true)
  public void get(
      String url,
      String tag,
      OnStringResponseListener onResponse,
      OnErrorListener onError,
      boolean noLoadingProgress
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.GET,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          tag,
          noLoadingProgress,
          onLoadingListener
      );
      if (!noLoadingProgress) {
        onRequestLoading();
      }
      requestQueue.add(request);
    });
  }

  // for single requests without a queue
  public void get(
      String url,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    get(url, uuidHelper, onResponse, onError);
  }

  // GET requests with modified user-agent
  public void get(
      String url,
      OnStringResponseListener onResponse,
      OnErrorListener onError,
      String userAgent
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.GET,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper,
          userAgent
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void post(
      String url,
      JSONObject json,
      OnJSONResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomJsonObjectRequest request = new CustomJsonObjectRequest(
          Request.Method.POST,
          url,
          apiKey,
          sessionKey,
          json,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void postWithArray(
      String url,
      JSONObject json,
      OnJSONArrayResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomJsonArrayRequest request = new CustomJsonArrayRequest(
          Request.Method.POST,
          url,
          apiKey,
          sessionKey,
          json,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void post(String url, OnStringResponseListener onResponse, OnErrorListener onError) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.POST,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void postHassIngress(
      String url,
      JSONObject json,
      OnJSONResponseListener onResponse,
      OnErrorListener onError
  ) {
    CustomJsonObjectRequest request = new CustomJsonObjectRequest(
        url,
        hassLongLivedAccessToken,
        json,
        onResponse::onResponse,
        onError::onError,
        this::onRequestFinished,
        timeoutSeconds,
        uuidHelper
    );
    onRequestLoading();
    requestQueue.add(request);
  }

  public void put(
      String url,
      JSONObject json,
      OnJSONResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomJsonObjectRequest request = new CustomJsonObjectRequest(
          Request.Method.PUT,
          url,
          apiKey,
          sessionKey,
          json,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void delete(
      String url,
      String tag,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.DELETE,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          tag
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void delete(
      String url,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    delete(url, uuidHelper, onResponse, onError);
  }

  public QueueItem getProductGroups(
      OnProductGroupsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
            uuid,
            response -> {
              Type type = new TypeToken<List<ProductGroup>>() {
              }.getType();
              ArrayList<ProductGroup> productGroups = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download ProductGroups: " + productGroups);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(productGroups);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem updateProductGroups(
      String dbChangedTime,
      OnProductGroupsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
              uuid,
              response -> {
                Type type = new TypeToken<List<ProductGroup>>() {
                }.getType();
                ArrayList<ProductGroup> productGroups = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download ProductGroups: " + productGroups);
                }
                Single.concat(
                    appDatabase.productGroupDao().deleteProductGroups(),
                    appDatabase.productGroupDao().insertProductGroups(productGroups)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, dbChangedTime).apply();
                      onResponseListener.onResponse(productGroups);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ProductGroups download");
      }
      return null;
    }
  }

  public QueueItem getQuantityUnits(
      OnQuantityUnitsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
            uuid,
            response -> {
              Type type = new TypeToken<List<QuantityUnit>>() {
              }.getType();
              ArrayList<QuantityUnit> quantityUnits = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download QuantityUnits: " + quantityUnits);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(quantityUnits);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem updateQuantityUnits(
      String dbChangedTime,
      OnQuantityUnitsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
              uuid,
              response -> {
                Type type = new TypeToken<List<QuantityUnit>>() {
                }.getType();
                ArrayList<QuantityUnit> quantityUnits = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download QuantityUnits: " + quantityUnits);
                }
                Single.concat(
                    appDatabase.quantityUnitDao().deleteQuantityUnits(),
                    appDatabase.quantityUnitDao().insertQuantityUnits(quantityUnits)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, dbChangedTime).apply();
                      onResponseListener.onResponse(quantityUnits);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped QuantityUnits download");
      }
      return null;
    }
  }

  public QueueItem updateQuantityUnitConversions(
      String dbChangedTime,
      OnQuantityUnitConversionsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNIT_CONVERSIONS),
              uuid,
              response -> {
                Type type = new TypeToken<List<QuantityUnitConversion>>() {
                }.getType();
                ArrayList<QuantityUnitConversion> conversions
                    = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download QuantityUnitConversions: "
                      + conversions);
                }
                Single.concat(
                    appDatabase.quantityUnitConversionDao().deleteConversions(),
                    appDatabase.quantityUnitConversionDao().insertConversions(conversions)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, dbChangedTime).apply();
                      onResponseListener.onResponse(conversions);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped QuantityUnitConversions download");
      }
      return null;
    }
  }

  public QueueItem getLocations(
      OnLocationsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
            uuid,
            response -> {
              Type type = new TypeToken<List<Location>>() {
              }.getType();
              ArrayList<Location> locations = gson.fromJson(response, type);
              if (debug) {
                Log.i(tag, "download Locations: " + locations);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(locations);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem updateLocations(
      String dbChangedTime,
      OnLocationsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_LOCATIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
              uuid,
              response -> {
                Type type = new TypeToken<List<Location>>() {
                }.getType();
                ArrayList<Location> locations = gson.fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Locations: " + locations);
                }
                Single.concat(
                    appDatabase.locationDao().deleteLocations(),
                    appDatabase.locationDao().insertLocations(locations)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, dbChangedTime).apply();
                      onResponseListener.onResponse(locations);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Locations download");
      }
      return null;
    }
  }

  public QueueItem updateStockCurrentLocations(
      String dbChangedTime,
      OnStockLocationsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STOCK_LOCATIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.STOCK_CURRENT_LOCATIONS),
              uuid,
              response -> {
                Type type = new TypeToken<List<StockLocation>>() {
                }.getType();
                ArrayList<StockLocation> locations = gson.fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download StockCurrentLocations: " + locations);
                }
                Single.concat(
                    appDatabase.stockLocationDao().deleteStockLocations(),
                    appDatabase.stockLocationDao().insertStockLocations(locations)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_STOCK_LOCATIONS, dbChangedTime).apply();
                      onResponseListener.onResponse(locations);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped StockCurrentLocations download");
      }
      return null;
    }
  }

  public QueueItem updateProducts(
      String dbChangedTime,
      OnProductsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCTS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
              uuid,
              response -> {
                Type type = new TypeToken<List<Product>>() {
                }.getType();
                ArrayList<Product> products = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Products: " + products);
                }
                Single.concat(
                    appDatabase.productDao().deleteProducts(),
                    appDatabase.productDao().insertProducts(products)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, dbChangedTime).apply();
                      onResponseListener.onResponse(products);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Products download");
      }
      return null;
    }
  }

  public QueueItem updateProductsLastPurchased(
      String dbChangedTime,
      OnProductsLastPurchasedResponseListener onResponseListener,
      boolean isOptional
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_PRODUCTS_LAST_PURCHASED, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
                @Nullable OnStringResponseListener responseListener,
                @Nullable OnErrorListener errorListener,
                @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.PRODUCTS_LAST_PURCHASED),
              uuid,
              response -> {
                Type type = new TypeToken<List<ProductLastPurchased>>() {
                }.getType();
                ArrayList<ProductLastPurchased> productsLastPurchased = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download ProductsLastPurchased: " + productsLastPurchased);
                }
                Single.concat(
                    appDatabase.productLastPurchasedDao().deleteProductsLastPurchased(),
                    appDatabase.productLastPurchasedDao()
                        .insertProductsLastPurchased(productsLastPurchased)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_PRODUCTS_LAST_PURCHASED, dbChangedTime).apply();
                      onResponseListener.onResponse(productsLastPurchased);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (isOptional) {
                  onResponseListener.onResponse(null);
                  if (responseListener != null) {
                    responseListener.onResponse(null);
                  }
                  return;
                }
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ProductsLastPurchased download");
      }
      return null;
    }
  }

  public QueueItem updateProductsAveragePrice(
      String dbChangedTime,
      OnProductsAveragePriceResponseListener onResponseListener,
      boolean isOptional
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_PRODUCTS_AVERAGE_PRICE, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.PRODUCTS_AVERAGE_PRICE),
              uuid,
              response -> {
                Type type = new TypeToken<List<ProductAveragePrice>>() {
                }.getType();
                ArrayList<ProductAveragePrice> productsAveragePrice = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download ProductsAveragePrice: " + productsAveragePrice);
                }
                Single.concat(
                    appDatabase.productAveragePriceDao().deleteProductsAveragePrice(),
                    appDatabase.productAveragePriceDao()
                        .insertProductsAveragePrice(productsAveragePrice)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_PRODUCTS_AVERAGE_PRICE, dbChangedTime).apply();
                      onResponseListener.onResponse(productsAveragePrice);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (isOptional) {
                  onResponseListener.onResponse(null);
                  if (responseListener != null) {
                    responseListener.onResponse(null);
                  }
                  return;
                }
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ProductsAveragePrice download");
      }
      return null;
    }
  }

  public QueueItem updateProductBarcodes(
      String dbChangedTime,
      OnProductBarcodesResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_BARCODES),
              uuid,
              response -> {
                Type type = new TypeToken<List<ProductBarcode>>() {
                }.getType();
                ArrayList<ProductBarcode> barcodes
                    = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Barcodes: " + barcodes);
                }
                Single.concat(
                    appDatabase.productBarcodeDao().deleteProductBarcodes(),
                    appDatabase.productBarcodeDao().insertProductBarcodes(barcodes)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, dbChangedTime).apply();
                      onResponseListener.onResponse(barcodes);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ProductsBarcodes download");
      }
      return null;
    }
  }

  public QueueItemJson addProductBarcode(
      JSONObject jsonObject,
      OnResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItemJson() {
      @Override
      public void perform(
          @Nullable OnJSONResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        post(
            grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_BARCODES),
            jsonObject,
            response -> {
              if (debug) {
                Log.i(tag, "added ProductBarcode");
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse();
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getSingleFilteredProductBarcode(
      String barcode,
      OnProductBarcodeResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjectsEqualValue(
                GrocyApi.ENTITY.PRODUCT_BARCODES, "barcode", barcode
            ),
            uuid,
            response -> {
              Type type = new TypeToken<List<ProductBarcode>>() {
              }.getType();
              ArrayList<ProductBarcode> barcodes
                  = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download filtered Barcodes: " + barcodes);
              }
              if (onResponseListener != null) {
                ProductBarcode barcode = !barcodes.isEmpty()
                    ? barcodes.get(0) : null; // take first object
                onResponseListener.onResponse(barcode);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStockItems(
      OnStockItemsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStock(),
            uuid,
            response -> {
              Type type = new TypeToken<List<StockItem>>() {
              }.getType();
              ArrayList<StockItem> stockItems = gson.fromJson(response, type);
              if (debug) {
                Log.i(tag, "download StockItems: " + stockItems);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(stockItems);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem updateStockItems(
      String dbChangedTime,
      OnStockItemsResponseListener onResponseListener
  ) {
    OnStockItemsResponseListener newOnResponseListener = stockItems -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(
          Constants.PREF.DB_LAST_TIME_STOCK_ITEMS, dbChangedTime
      );
      editPrefs.apply();
      onResponseListener.onResponse(stockItems);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STOCK_ITEMS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getStockItems(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped StockItems download");
      }
      return null;
    }
  }

  public QueueItem getVolatile(
      OnVolatileResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStockVolatile(),
            uuid,
            response -> {
              if (debug) {
                Log.i(tag, "download Volatile: success");
              }
              ArrayList<StockItem> dueItems = new ArrayList<>();
              ArrayList<StockItem> overdueItems = new ArrayList<>();
              ArrayList<StockItem> expiredItems = new ArrayList<>();
              ArrayList<MissingItem> missingItems = new ArrayList<>();
              try {
                JSONObject jsonObject = new JSONObject(response);
                // Parse first part of volatile array: expiring products
                dueItems = gson.fromJson(
                    jsonObject.getJSONArray("due_products").toString(),
                    new TypeToken<List<StockItem>>() {
                    }.getType()
                );
                if (debug) {
                  Log.i(tag, "download Volatile: due = " + dueItems);
                }
                // Parse second part of volatile array: overdue products
                overdueItems = gson.fromJson(
                    jsonObject.getJSONArray("overdue_products").toString(),
                    new TypeToken<List<StockItem>>() {
                    }.getType()
                );
                if (debug) {
                  Log.i(tag, "download Volatile: overdue = " + overdueItems);
                }
                // Parse third part of volatile array: expired products
                expiredItems = gson.fromJson(
                    jsonObject.getJSONArray("expired_products").toString(),
                    new TypeToken<List<StockItem>>() {
                    }.getType()
                );
                if (debug) {
                  Log.i(tag, "download Volatile: expired = " + overdueItems);
                }
                // Parse fourth part of volatile array: missing products
                missingItems = gson.fromJson(
                    jsonObject.getJSONArray("missing_products").toString(),
                    new TypeToken<List<MissingItem>>() {
                    }.getType()
                );
                if (debug) {
                  Log.i(tag, "download Volatile: missing = " + missingItems);
                }
              } catch (JSONException e) {
                if (debug) {
                  Log.e(tag, "download Volatile: " + e);
                }
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(dueItems, overdueItems,
                    expiredItems, missingItems);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem updateVolatile(
      String dbChangedTime,
      OnVolatileResponseListener onResponseListener
  ) {
    OnVolatileResponseListener newOnResponseListener = (due, overdue, expired, missing) -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(
          Constants.PREF.DB_LAST_TIME_VOLATILE, dbChangedTime
      );
      editPrefs.apply();
      onResponseListener.onResponse(due, overdue, expired, missing);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_VOLATILE, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getVolatile(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Volatile download");
      }
      return null;
    }
  }

  public QueueItem updateMissingItems(
      String dbChangedTime,
      OnMissingItemsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getStockVolatile(),
              uuid,
              response -> {
                if (debug) {
                  Log.i(tag, "download Volatile (only missing): success");
                }
                ArrayList<MissingItem> missingItems = new ArrayList<>();
                try {
                  JSONObject jsonObject = new JSONObject(response);
                  // Parse fourth part of volatile array: missing products
                  missingItems = gson.fromJson(
                      jsonObject.getJSONArray("missing_products").toString(),
                      new TypeToken<List<MissingItem>>() {
                      }.getType()
                  );
                  if (debug) {
                    Log.i(tag, "download Volatile (only missing): missing = " + missingItems);
                  }

                } catch (JSONException e) {
                  if (debug) {
                    Log.e(tag, "download Volatile (only missing): " + e);
                  }
                }
                ArrayList<MissingItem> finalMissingItems = missingItems;
                Single.concat(
                    appDatabase.missingItemDao().deleteMissingItems(),
                    appDatabase.missingItemDao().insertMissingItems(missingItems)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, dbChangedTime).apply();
                      onResponseListener.onResponse(finalMissingItems);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped MissingItems download");
      }
      return null;
    }
  }

  public QueueItem getProductDetails(
      int productId,
      OnProductDetailsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStockProductDetails(productId),
            uuid,
            response -> {
              Type type = new TypeToken<ProductDetails>() {
              }.getType();
              ProductDetails productDetails = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download ProductDetails: " + productDetails);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(productDetails);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getProductDetails(
      int productId,
      OnProductDetailsResponseListener onResponseListener
  ) {
    return getProductDetails(productId, onResponseListener, null);
  }

  public QueueItem getStockLocations(
      int productId,
      OnStockLocationsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStockLocationsFromProduct(productId),
            uuid,
            response -> {
              Type type = new TypeToken<ArrayList<StockLocation>>() {
              }.getType();
              ArrayList<StockLocation> stockLocations = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download StockLocations: " + stockLocations);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(stockLocations);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStockLocations(
      int productId,
      OnStockLocationsResponseListener onResponseListener
  ) {
    return getStockLocations(productId, onResponseListener, null);
  }

  public QueueItem getStockEntries(
      int productId,
      OnStockEntriesResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStockEntriesFromProduct(productId),
            uuid,
            response -> {
              Type type = new TypeToken<ArrayList<StockEntry>>() {
              }.getType();
              ArrayList<StockEntry> stockEntries = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download StockEntries: " + stockEntries);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(stockEntries);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStockEntries(
      int productId,
      OnStockEntriesResponseListener onResponseListener
  ) {
    return getStockEntries(productId, onResponseListener, null);
  }

  public QueueItem updateShoppingListItems(
      String dbChangedTime,
      OnShoppingListItemsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
              uuid,
              response -> {
                Type type = new TypeToken<List<ShoppingListItem>>() {
                }.getType();
                ArrayList<ShoppingListItem> shoppingListItems = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download ShoppingListItems: " + shoppingListItems);
                }
                Single.concat(
                    appDatabase.shoppingListItemDao().deleteShoppingListItems(),
                    appDatabase.shoppingListItemDao().insertShoppingListItems(shoppingListItems)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, dbChangedTime).apply();
                      onResponseListener.onResponse(shoppingListItems);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ShoppingListItems download");
      }
      return null;
    }
  }

  public QueueItem updateShoppingListItems(
      String dbChangedTime,
      OnShoppingListItemsWithSyncResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
              uuid,
              response -> {
                Type type = new TypeToken<List<ShoppingListItem>>() {
                }.getType();
                ArrayList<ShoppingListItem> shoppingListItems = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download ShoppingListItems: " + shoppingListItems);
                }
                ArrayList<ShoppingListItem> itemsToSync = new ArrayList<>();
                HashMap<Integer, ShoppingListItem> serverItemsHashMap = new HashMap<>();
                for (ShoppingListItem s : shoppingListItems) {
                  serverItemsHashMap.put(s.getId(), s);
                }

                appDatabase.shoppingListItemDao().getShoppingListItems()
                    .doOnSuccess(offlineItems -> {
                      // compare server items with offline items and add modified to separate list
                      for (ShoppingListItem offlineItem : offlineItems) {
                        ShoppingListItem serverItem = serverItemsHashMap.get(offlineItem.getId());
                        if (serverItem != null  // sync only items which are still on server
                            && offlineItem.getDoneSynced() != -1
                            && offlineItem.getDoneInt() != offlineItem.getDoneSynced()
                            && offlineItem.getDoneInt() != serverItem.getDoneInt()
                            || serverItem != null
                            && serverItem.getDoneSynced() != -1  // server database hasn't changed
                            && offlineItem.getDoneSynced() != -1
                            && offlineItem.getDoneInt() != offlineItem.getDoneSynced()
                        ) {
                          itemsToSync.add(offlineItem);
                        }
                      }
                    })
                    .doFinally(() -> {
                      appDatabase.shoppingListItemDao().deleteAll();
                      appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, dbChangedTime).apply();
                      onResponseListener.onResponse(shoppingListItems, itemsToSync, serverItemsHashMap);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ShoppingListItems download");
      }
      return null;
    }
  }

  public QueueItem updateShoppingLists(
      String dbChangedTime,
      OnShoppingListsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LISTS),
              uuid,
              response -> {
                Type type = new TypeToken<List<ShoppingList>>() {
                }.getType();
                ArrayList<ShoppingList> shoppingLists = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download ShoppingLists: " + shoppingLists);
                }
                Single.concat(
                    appDatabase.shoppingListDao().deleteShoppingLists(),
                    appDatabase.shoppingListDao().insertShoppingLists(shoppingLists)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_SHOPPING_LISTS, dbChangedTime).apply();
                      onResponseListener.onResponse(shoppingLists);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ShoppingLists download");
      }
      return null;
    }
  }

  public QueueItem updateStores(
      String dbChangedTime,
      OnStoresResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STORES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(GrocyApi.ENTITY.STORES),
              uuid,
              response -> {
                Type type = new TypeToken<List<Store>>() {
                }.getType();
                ArrayList<Store> stores = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Stores: " + stores);
                }
                Single.concat(
                    appDatabase.storeDao().deleteStores(),
                    appDatabase.storeDao().insertStores(stores)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_STORES, dbChangedTime).apply();
                      onResponseListener.onResponse(stores);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Stores download");
      }
      return null;
    }
  }

  public QueueItem editShoppingListItem(
      int itemId,
      JSONObject body,
      OnJSONResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        put(
            grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, itemId),
            body,
            response -> {
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
              }
              if (responseListener != null) {
                responseListener.onResponse(null);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem editShoppingListItem(int itemId, JSONObject body) {
    return editShoppingListItem(itemId, body, null, null);
  }

  public QueueItem deleteShoppingListItem(
      int itemId,
      OnStringResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        delete(
            grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, itemId),
            uuid,
            response -> {
              if (debug) {
                Log.i(tag, "delete ShoppingListItem: " + itemId);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem deleteShoppingListItem(int itemId) {
    return deleteShoppingListItem(itemId, null, null);
  }

  public QueueItem updateTasks(
      String dbChangedTime,
      OnTasksResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_TASKS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.TASKS),
              uuid,
              response -> {
                Type type = new TypeToken<List<Task>>() {
                }.getType();
                ArrayList<Task> tasks = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Tasks: " + tasks);
                }
                Single.concat(
                    appDatabase.taskDao().deleteTasks(),
                    appDatabase.taskDao().insertTasks(tasks)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_TASKS, dbChangedTime).apply();
                      onResponseListener.onResponse(tasks);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Tasks download");
      }
      return null;
    }
  }

  public QueueItem updateTaskCategories(
      String dbChangedTime,
      OnTaskCategoriesResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_TASK_CATEGORIES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.TASK_CATEGORIES),
              uuid,
              response -> {
                Type type = new TypeToken<List<TaskCategory>>() {
                }.getType();
                ArrayList<TaskCategory> taskCategories = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Task categories: " + taskCategories);
                }
                Single.concat(
                    appDatabase.taskCategoryDao().deleteCategories(),
                    appDatabase.taskCategoryDao().insertCategories(taskCategories)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_TASK_CATEGORIES, dbChangedTime).apply();
                      onResponseListener.onResponse(taskCategories);
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped TaskCategories download");
      }
      return null;
    }
  }

  public QueueItem getSystemInfo(
      OnStringResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getSystemInfo(),
            uuid,
            response -> {
              if (debug) {
                Log.i(tag, "get systemInfo: " + response);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public void getTimeDbChanged(
      OnStringResponseListener onResponseListener,
      OnSimpleErrorListener onErrorListener
  ) {
    get(
        grocyApi.getDbChangedTime(),
        uuidHelper,
        response -> {
          try {
            JSONObject body = new JSONObject(response);
            String dateStr = body.getString("changed_time");
            onResponseListener.onResponse(dateStr);
          } catch (JSONException e) {
            if (debug) {
              Log.e(tag, "getTimeDbChanged: " + e);
            }
            onErrorListener.onError();
          }
        },
        error -> onErrorListener.onError(),
        !sharedPrefs.getBoolean(
            Constants.SETTINGS.NETWORK.LOADING_CIRCLE,
            Constants.SETTINGS_DEFAULT.NETWORK.LOADING_CIRCLE
        )
    );
  }

  public void uploadSetting(
      String settingKey,
      Object settingValue,
      OnSettingUploadListener listener
  ) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("value", settingValue);
    } catch (JSONException e) {
      e.printStackTrace();
      listener.onFinished(R.string.option_synced_error);
      return;
    }
    put(
        grocyApi.getUserSetting(settingKey),
        jsonObject,
        response -> listener.onFinished(R.string.option_synced_success),
        volleyError -> listener.onFinished(R.string.option_synced_error)
    );
  }

  public void getOpenFoodFactsProductName(
      String barcode,
      OnStringResponseListener successListener,
      OnErrorListener errorListener
  ) {
    get(
        OpenFoodFactsApi.getProduct(barcode),
        response -> {
          String language = application.getResources().getConfiguration().locale.getLanguage();
          String country = application.getResources().getConfiguration().locale.getCountry();
          String both = language + "_" + country;
          if(debug) Log.i(tag, "getOpenFoodFactsProductName: locale = " + both);
          try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject product = jsonObject.getJSONObject("product");
            String name = product.optString("product_name_" + both);
            if(name.isEmpty()) {
              name = product.optString("product_name_" + language);
            }
            if(name.isEmpty()) {
              name = product.optString("product_name");
            }
            successListener.onResponse(name);
            if(debug) Log.i(tag, "getOpenFoodFactsProductName: OpenFoodFacts = " + name);
          } catch (JSONException e) {
            if(debug) Log.e(tag, "getOpenFoodFactsProductName: " + e);
            successListener.onResponse(null);
          }
        },
        error -> {
          if(debug) Log.e(tag, "getOpenFoodFactsProductName: can't get OpenFoodFacts product");
          errorListener.onError(error);
        },
        OpenFoodFactsApi.getUserAgent(application)
    );
  }

  public void getOpenBeautyFactsProductName(
      String barcode,
      OnStringResponseListener successListener,
      OnErrorListener errorListener
  ) {
    get(
        OpenBeautyFactsApi.getProduct(barcode),
        response -> {
          String language = application.getResources().getConfiguration().locale.getLanguage();
          String country = application.getResources().getConfiguration().locale.getCountry();
          String both = language + "_" + country;
          if(debug) Log.i(tag, "getOpenBeautyFactsProductName: locale = " + both);
          try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject product = jsonObject.getJSONObject("product");
            String name = product.optString("product_name_" + both);
            if(name.isEmpty()) {
              name = product.optString("product_name_" + language);
            }
            if(name.isEmpty()) {
              name = product.optString("product_name");
            }
            successListener.onResponse(name);
            if(debug) Log.i(tag, "getOpenBeautyFactsProductName: OpenBeautyFacts = " + name);
          } catch (JSONException e) {
            if(debug) Log.e(tag, "getOpenBeautyFactsProductName: " + e);
            successListener.onResponse(null);
          }
        },
        error -> {
          if(debug) Log.e(tag, "getOpenBeautyFactsProductName: can't get OpenBeautyFacts product");
          errorListener.onError(error);
        },
        OpenBeautyFactsApi.getUserAgent(application)
    );
  }

  public QueueItem getStringData(
      String url,
      OnStringResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            url,
            uuid,
            response -> {
              if (debug) {
                Log.i(
                    tag,
                    "download StringData from " + url + " : " + response
                );
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (debug) {
                Log.e(tag, "download StringData: " + error);
              }
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStringData(String url, OnStringResponseListener onResponseListener) {
    return getStringData(url, onResponseListener, null);
  }

  public void checkHassLongLivedToken(OnStringResponseListener onResponseListener) {
    postHassIngress(
        hassServerUrl + "/api/hassio/ingress/session",
        null,
        response -> {
          try {
            boolean isOk = response.get("result").equals("ok");
            onResponseListener.onResponse(isOk ? (String) response.get("result") : null);
          } catch (JSONException e) {
            Log.e(tag,
                "checkHassLongLivedToken (/api/hassio/ingress/session): JSONException:");
            e.printStackTrace();
            onResponseListener.onResponse(null);
          }
        },
        error -> {
          Log.e(tag, "checkHassLongLivedToken (/api/hassio/ingress/session): error: " + error);
          onResponseListener.onResponse(null);
        }
    );
  }

  public void validateHassIngressSessionIfNecessary(OnStringResponseListener onFinishedListener) {
    validateHassIngressSessionIfNecessary(onFinishedListener, onFinishedListener);
  }

  public void validateHassIngressSessionIfNecessary(
      OnStringResponseListener onSuccessListener,
      OnStringResponseListener onErrorListener
  ) {
    if (hassServerUrl == null || hassServerUrl.isEmpty()) {
      onSuccessListener.onResponse(null);
      return;
    }

    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    String sessionKeyTimeStr = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME, null);

    if (sessionKey != null && dateUtil.isTimeLessThanOneMinuteAway(sessionKeyTimeStr)) {
      onSuccessListener.onResponse(sessionKey);
      return;
    }

    homeAssistantSessionAuth(sessionKey, onSuccessListener, onErrorListener);
  }

  private void homeAssistantSessionAuth(
      String sessionOld,
      OnStringResponseListener onSuccessListener,
      OnStringResponseListener onErrorListener
  ) {
    String hassUrlExtension;
    JSONObject jsonObject = null;
    if (sessionOld != null) {
      hassUrlExtension = "/api/hassio/ingress/validate_session";
      try {
        jsonObject = new JSONObject();
        jsonObject.put("session", sessionOld);
      } catch (JSONException e) {
        Log.e(tag, "homeAssistantSessionAuth: JSONException1:");
        e.printStackTrace();
      }
    } else {
      hassUrlExtension = "/api/hassio/ingress/session";
    }

    postHassIngress(
        hassServerUrl + hassUrlExtension,
        jsonObject,
        response -> {
          try {
            boolean isOk = response.get("result").equals("ok");
            JSONObject data = isOk && response.has("data") ? response.getJSONObject("data") : null;
            String session = data != null && data.has("session") ? data.getString("session") : null;
            if (session != null) {
              sharedPrefs.edit().putString(
                  Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY,
                  session
              ).putString(
                  Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME,
                  dateUtil.getCurrentDateWithTimeStr()
              ).apply();
              onSuccessListener.onResponse(session);
            } else if (isOk && sessionOld != null) {
              sharedPrefs.edit().putString(
                  Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME,
                  dateUtil.getCurrentDateWithTimeStr()
              ).apply();
              onSuccessListener.onResponse(sessionOld);
            } else {
              Log.e(tag, "homeAssistantSessionAuth: " + hassUrlExtension + ": bad response: " + response);
              onErrorListener.onResponse(null);
            }
          } catch (JSONException e) {
            Log.e(tag, "homeAssistantSessionAuth: " + hassUrlExtension + ": JSONException2: ");
            e.printStackTrace();
            onErrorListener.onResponse(null);
          }
        },
        error -> {
          Log.e(tag, "homeAssistantSessionAuth: " + hassUrlExtension + ": error: " + error);
          if (sessionOld != null && error instanceof AuthFailureError) {
            homeAssistantSessionAuth(null, onSuccessListener, onErrorListener);
            return;
          }
          onErrorListener.onResponse(null);
        }
    );
  }

  public class Queue {

    private final ArrayList<QueueItem> queueItems;
    private final OnQueueEmptyListener onQueueEmptyListener;
    private final OnErrorListener onErrorListener;
    private final String uuidQueue;
    private int queueSize;
    private boolean isRunning;

    public Queue(OnQueueEmptyListener onQueueEmptyListener, OnErrorListener onErrorListener) {
      this.onQueueEmptyListener = onQueueEmptyListener;
      this.onErrorListener = onErrorListener;
      queueItems = new ArrayList<>();
      uuidQueue = UUID.randomUUID().toString();
      queueSize = 0;
      isRunning = false;
    }

    public Queue append(QueueItem... queueItems) {
      for (QueueItem queueItem : queueItems) {
        if (queueItem == null) {
          continue;
        }
        this.queueItems.add(queueItem);
        queueSize++;
      }
      return this;
    }

    public void start() {
      if (isRunning) {
        return;
      } else {
        isRunning = true;
      }
      if (queueItems.isEmpty()) {
        if (onQueueEmptyListener != null) {
          onQueueEmptyListener.execute();
        }
        return;
      }
      while (!queueItems.isEmpty()) {
        QueueItem queueItem = queueItems.remove(0);
        queueItem.perform(response -> {
          queueSize--;
          if (queueSize > 0) {
            return;
          }
          isRunning = false;
          if (onQueueEmptyListener != null) {
            onQueueEmptyListener.execute();
          }
          reset(false);
        }, error -> {
          isRunning = false;
          if (onErrorListener != null) {
            onErrorListener.onError(error);
          }
          reset(true);
        }, uuidQueue);
      }
    }

    public int getSize() {
      return queueSize;
    }

    public boolean isEmpty() {
      return queueSize == 0;
    }

    public void reset(boolean cancelAll) {
      if (cancelAll) {
        requestQueue.cancelAll(uuidQueue);
      }
      queueItems.clear();
      queueSize = 0;
    }
  }

  public Queue newQueue(
      OnQueueEmptyListener onQueueEmptyListener,
      OnErrorListener onErrorListener
  ) {
    Queue queue = new Queue(onQueueEmptyListener, onErrorListener);
    queueArrayList.add(queue);
    return queue;
  }

  public abstract static class BaseQueueItem {

  }

  public abstract static class QueueItem extends BaseQueueItem {

    public abstract void perform(
        OnStringResponseListener responseListener,
        OnErrorListener errorListener,
        String uuid
    );

    public void perform(String uuid) {
      // UUID is for cancelling the requests; should be uuidHelper from above
      perform(null, null, uuid);
    }
  }

  public abstract static class QueueItemJson extends BaseQueueItem {

    public abstract void perform(
        OnJSONResponseListener responseListener,
        OnErrorListener errorListener,
        String uuid
    );

    public void perform(String uuid) {
      // UUID is for cancelling the requests; should be uuidHelper from above
      perform(null, null, uuid);
    }
  }

  public interface OnObjectsResponseListener {

    void onResponse(ArrayList<Object> objects);
  }

  public interface OnProductGroupsResponseListener {

    void onResponse(ArrayList<ProductGroup> productGroups);
  }

  public interface OnQuantityUnitsResponseListener {

    void onResponse(ArrayList<QuantityUnit> quantityUnits);
  }

  public interface OnQuantityUnitConversionsResponseListener {

    void onResponse(ArrayList<QuantityUnitConversion> quantityUnitConversions);
  }

  public interface OnLocationsResponseListener {

    void onResponse(ArrayList<Location> locations);
  }

  public interface OnProductsResponseListener {

    void onResponse(ArrayList<Product> products);
  }

  public interface OnProductsLastPurchasedResponseListener {

    void onResponse(ArrayList<ProductLastPurchased> productsLastPurchased);
  }

  public interface OnProductsAveragePriceResponseListener {

    void onResponse(ArrayList<ProductAveragePrice> productsAveragePrice);
  }

  public interface OnProductBarcodesResponseListener {

    void onResponse(ArrayList<ProductBarcode> productBarcodes);
  }

  public interface OnProductBarcodeResponseListener {

    void onResponse(ProductBarcode productBarcode);
  }

  public interface OnStockItemsResponseListener {

    void onResponse(ArrayList<StockItem> stockItems);
  }

  public interface OnVolatileResponseListener {

    void onResponse(
        ArrayList<StockItem> due,
        ArrayList<StockItem> overdue,
        ArrayList<StockItem> expired,
        ArrayList<MissingItem> missing
    );
  }

  public interface OnMissingItemsResponseListener {

    void onResponse(ArrayList<MissingItem> missingItems);
  }

  public interface OnProductDetailsResponseListener {

    void onResponse(ProductDetails productDetails);
  }

  public interface OnStockLocationsResponseListener {

    void onResponse(ArrayList<StockLocation> stockLocations);
  }

  public interface OnStockEntriesResponseListener {

    void onResponse(ArrayList<StockEntry> stockEntries);
  }

  public interface OnShoppingListItemsResponseListener {

    void onResponse(ArrayList<ShoppingListItem> shoppingListItems);
  }

  public interface OnShoppingListItemsWithSyncResponseListener {

    void onResponse(
        ArrayList<ShoppingListItem> shoppingListItems,
        ArrayList<ShoppingListItem> itemsToSync,
        HashMap<Integer, ShoppingListItem> serverItemHashMap
    );
  }

  public interface OnShoppingListsResponseListener {

    void onResponse(ArrayList<ShoppingList> shoppingLists);
  }

  public interface OnStoresResponseListener {

    void onResponse(ArrayList<Store> stores);
  }

  public interface OnTasksResponseListener {

    void onResponse(ArrayList<Task> tasks);
  }

  public interface OnTaskCategoriesResponseListener {

    void onResponse(ArrayList<TaskCategory> taskCategories);
  }

  public interface OnStringResponseListener {

    void onResponse(String response);
  }

  public interface OnJSONResponseListener {

    void onResponse(JSONObject response);
  }

  public interface OnJSONArrayResponseListener {

    void onResponse(JSONArray response);
  }

  public interface OnResponseListener {

    void onResponse();
  }

  public interface OnErrorListener {

    void onError(VolleyError volleyError);
  }

  public interface OnSimpleErrorListener {

    void onError();
  }

  public interface OnQueueEmptyListener {

    void execute();
  }

  public interface OnLoadingListener {

    void onLoadingChanged(boolean isLoading);
  }

  public interface OnSettingUploadListener {

    void onFinished(@StringRes int msg);
  }
}
