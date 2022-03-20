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

import android.app.Application;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import java.util.ArrayList;
import de.osamaco.grocy.R;

public class FilterChipLiveDataTasksStatus extends FilterChipLiveData {

  public final static int STATUS_ALL = 0;
  public final static int STATUS_DUE_SOON = 1;
  public final static int STATUS_OVERDUE = 2;

  private final Application application;
  private int dueSoonCount = 0;
  private int overdueCount = 0;

  public FilterChipLiveDataTasksStatus(Application application, Runnable clickListener) {
    this.application = application;
    setStatus(STATUS_ALL, null);
    if (clickListener != null) {
      setMenuItemClickListener(item -> {
        setStatus(item.getItemId(), item.getTitle().toString());
        emitValue();
        clickListener.run();
        return true;
      });
    }
  }

  public int getStatus() {
    return getItemIdChecked();
  }

  public FilterChipLiveDataTasksStatus setStatus(int status, @Nullable String text) {
    if (status == STATUS_ALL) {
      setActive(false);
      setText(application.getString(R.string.property_status));
    } else {
      setActive(true);
      assert text != null;
      setText(text);
    }
    setItemIdChecked(status);
    return this;
  }

  public FilterChipLiveDataTasksStatus setDueSoonCount(int dueSoonCount) {
    this.dueSoonCount = dueSoonCount;
    return this;
  }

  public FilterChipLiveDataTasksStatus setOverdueCount(int overdueCount) {
    this.overdueCount = overdueCount;
    return this;
  }

  public void emitCounts() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        STATUS_ALL,
        0,
        application.getString(R.string.action_no_filter)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_DUE_SOON,
        0,
        getQuString(R.plurals.msg_due_tasks, dueSoonCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_OVERDUE,
        0,
        getQuString(R.plurals.msg_overdue_tasks, overdueCount)
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(new MenuItemGroup(0, true, true));
    for (MenuItemData menuItemData : menuItemDataList) {
      if (getItemIdChecked() != STATUS_ALL && getItemIdChecked() == menuItemData.getItemId()) {
        setText(menuItemData.getText());
      }
    }
    emitValue();
  }

  private String getQuString(@PluralsRes int string, int count) {
    return application.getResources().getQuantityString(string, count, count);
  }
}