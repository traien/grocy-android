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

package de.osamaco.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import de.osamaco.grocy.R;
import de.osamaco.grocy.api.GrocyApi;
import de.osamaco.grocy.helper.DownloadHelper;
import de.osamaco.grocy.model.FilterChipLiveData;
import de.osamaco.grocy.model.FilterChipLiveDataTasksSort;
import de.osamaco.grocy.model.FilterChipLiveDataTasksStatus;
import de.osamaco.grocy.model.InfoFullscreen;
import de.osamaco.grocy.model.SnackbarMessage;
import de.osamaco.grocy.model.Task;
import de.osamaco.grocy.model.TaskCategory;
import de.osamaco.grocy.repository.TasksRepository;
import de.osamaco.grocy.util.Constants;
import de.osamaco.grocy.util.PluralUtil;
import de.osamaco.grocy.util.PrefsUtil;
import de.osamaco.grocy.util.SortUtil;

public class TasksViewModel extends BaseViewModel {

  private final static String TAG = TasksViewModel.class.getSimpleName();
  public final static String SORT_NAME = "sort_name";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final TasksRepository repository;
  private final PluralUtil pluralUtil;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<Task>> filteredTasksLive;
  private final FilterChipLiveDataTasksStatus filterChipLiveDataStatus;
  private final FilterChipLiveDataTasksSort filterChipLiveDataSort;

  private List<Task> tasks;
  private List<TaskCategory> taskCategories;
  private HashMap<Integer, Task> taskHashMap;

  private DownloadHelper.Queue currentQueueLoading;
  private String searchInput;
  private String sortMode;
  private int tasksNotDoneCount;
  private int tasksDoneCount;
  private boolean sortAscending;
  private final boolean debug;

  public TasksViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new TasksRepository(application);
    pluralUtil = new PluralUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    filteredTasksLive = new MutableLiveData<>();

    filterChipLiveDataStatus = new FilterChipLiveDataTasksStatus(
        getApplication(),
        this::updateFilteredTasks
    );
    filterChipLiveDataSort = new FilterChipLiveDataTasksSort(
        getApplication(),
        this::updateFilteredTasks
    );
    tasksDoneCount = 0;
    tasksNotDoneCount = 0;
    sortMode = sharedPrefs.getString(Constants.PREF.STOCK_SORT_MODE, SORT_NAME);
    sortAscending = sharedPrefs.getBoolean(Constants.PREF.STOCK_SORT_ASCENDING, true);
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
          this.taskCategories = data.getTaskGroups();
          this.tasks = data.getTasks();
          taskHashMap = new HashMap<>();
          for (Task task : tasks) {
            taskHashMap.put(task.getId(), task);
          }

          tasksDoneCount = 0;
          tasksNotDoneCount = 0;
          for (Task task : tasks) {
            if (task.isDone()) {
              tasksDoneCount++;
            } else {
              tasksNotDoneCount++;
            }
          }

          filterChipLiveDataStatus
              .setDueSoonCount(0)
              .setOverdueCount(0)
              .emitCounts();

          updateFilteredTasks();
          if (downloadAfterLoading) {
            downloadData();
          }
        }
    );
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredTasks();
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(this::updateFilteredTasks, this::onDownloadError);
    queue.append(
        dlHelper.updateTaskCategories(
            dbChangedTime,
            taskCategories -> this.taskCategories = taskCategories
        ), dlHelper.updateTasks(dbChangedTime, tasks -> {
          this.tasks = tasks;
          taskHashMap = new HashMap<>();
          for (Task task : tasks) {
            taskHashMap.put(task.getId(), task);
          }
        })
    );

    if (queue.isEmpty()) {
      updateFilteredTasks();
      return;
    }

    currentQueueLoading = queue;
    queue.start();
  }

  public void downloadData() {
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_TASKS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_TASK_CATEGORIES, null);
    editPrefs.apply();
    downloadData();
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
    if (!isOffline()) {
      setOfflineLive(true);
    }
  }

  public void updateFilteredTasks() {
    ArrayList<Task> filteredTasks = new ArrayList<>();

    for (Task task : this.tasks) {

      boolean searchContainsItem = true;
      if (searchInput != null && !searchInput.isEmpty()) {
        searchContainsItem = task.getName().toLowerCase().contains(searchInput);
      }
      if (!searchContainsItem) {
        continue;
      }



      filteredTasks.add(task);
    }

    switch (sortMode) {
      case SORT_NAME:
        SortUtil.sortTasksByName(getApplication(), filteredTasks, sortAscending);
        break;
    }

    filteredTasksLive.setValue(filteredTasks);
  }

  public void performAction(String action, Task task) {
    switch (action) {
      case Constants.ACTION.COMPLETE:
        markTask(task, true);
        break;
      case Constants.ACTION.UNDO:
        markTask(task, false);
        break;
    }
  }

  private void markTask(Task task, boolean completed) {
    // TODO Not just the done_time?
    LocalDateTime done_time = LocalDateTime.now();
    JSONObject body = new JSONObject();
    try {
      body.put("done_time", done_time);
    } catch (JSONException e) {
      if (debug) {
        if (completed) {
          Log.e(TAG, "completeTask: " + e);
        } else {
          Log.e(TAG, "undoTask: " + e);
        }
      }
    }
    dlHelper.postWithArray(
        completed ? grocyApi.completeTask(task.getId()) : grocyApi.undoTask(task.getId()),
        body,
        response -> {
          String transactionId = null;
          try {
            transactionId = response.getJSONObject(0)
                .getString("transaction_id");
          } catch (JSONException e) {
            if (completed) {
              Log.e(TAG, "completeTask: " + e);
            } else {
              Log.e(TAG, "undoTask: " + e);
            }
          }

          String msg = getApplication().getString(
              completed ? R.string.msg_task_completed : R.string.msg_task_undo, task.getName()
          );
          SnackbarMessage snackbarMsg = new SnackbarMessage(msg, 15);

          downloadData();
          showSnackbar(snackbarMsg);
          if (completed) {
            Log.i(
                TAG, "completeTask: completed " + task.getName()
            );
          } else {
            Log.i(TAG, "undoTask: undone" + task.getName());
          }
        },
        error -> {
          showErrorMessage(error);
          if (debug) {
            if (completed) {
              Log.i(TAG, "completeTask: " + error);
            } else {
              Log.i(TAG, "undoTask: " + error);
            }
          }
        }
    );
  }

  public boolean isSearchActive() {
    return searchInput != null && !searchInput.isEmpty();
  }

  public void resetSearch() {
    searchInput = null;
    setIsSearchVisible(false);
  }

  public MutableLiveData<ArrayList<Task>> getFilteredTasksLive() {
    return filteredTasksLive;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataStatus() {
    return () -> filterChipLiveDataStatus;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataSort() {
    return () -> filterChipLiveDataSort;
  }

  public int getTasksDoneCount() {
    return tasksDoneCount;
  }

  public int getTasksNotDoneCount() {
    return tasksNotDoneCount;
  }

  public int getTasksDueCount() {
    // TODO
    return 0;
  }

  public int getTasksOverdueCount(){
    // TODO
    return 0;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();
    updateFilteredTasks();
  }

  public String getSortMode() {
    return sortMode;
  }

  public void setSortMode(String sortMode) {
    this.sortMode = sortMode;
    sharedPrefs.edit().putString(Constants.PREF.STOCK_SORT_MODE, sortMode).apply();
    updateFilteredTasks();
  }

  public boolean isSortAscending() {
    return sortAscending;
  }

  public void setSortAscending(boolean sortAscending) {
    this.sortAscending = sortAscending;
    sharedPrefs.edit().putBoolean(Constants.PREF.STOCK_SORT_ASCENDING, sortAscending).apply();
    updateFilteredTasks();
  }

  public HashMap<Integer, Task> getTaskHashMap() {
    return taskHashMap;
  }

  @NonNull
  public MutableLiveData<Boolean> getOfflineLive() {
    return offlineLive;
  }

  public Boolean isOffline() {
    return offlineLive.getValue();
  }

  public void setOfflineLive(boolean isOffline) {
    offlineLive.setValue(isOffline);
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
