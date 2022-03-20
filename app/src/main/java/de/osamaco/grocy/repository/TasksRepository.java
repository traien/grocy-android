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
import de.osamaco.grocy.model.Task;
import de.osamaco.grocy.model.TaskCategory;

public class TasksRepository {

  private final AppDatabase appDatabase;

  public TasksRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface TasksDataListener {

    void actionFinished(TasksData data);
  }

  public static class TasksData {

    private final List<TaskCategory> taskGroups;
    private final List<Task> tasks;

    public TasksData(
        List<TaskCategory> taskGroups,
        List<Task> tasks
    ) {
      this.taskGroups = taskGroups;
      this.tasks = tasks;
    }

    public List<TaskCategory> getTaskGroups() {
      return taskGroups;
    }

    public List<Task> getTasks() {
      return tasks;
    }
  }

  public void loadFromDatabase(TasksDataListener listener) {
    Single
        .zip(
            appDatabase.taskCategoryDao().getTaskCategories(),
            appDatabase.taskDao().getTasks(),
            TasksData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }
}
