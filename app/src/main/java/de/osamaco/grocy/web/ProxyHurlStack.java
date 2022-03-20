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

package de.osamaco.grocy.web;

import android.content.SharedPreferences;
import com.android.volley.toolbox.HurlStack;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import de.osamaco.grocy.util.Constants.SETTINGS.NETWORK;
import de.osamaco.grocy.util.Constants.SETTINGS_DEFAULT;

public class ProxyHurlStack extends HurlStack {

  private final Proxy proxy;

  public ProxyHurlStack(SharedPreferences sharedPrefs, boolean useTor) {
    super();

    if (useTor) {
      this.proxy = new Proxy(
          Proxy.Type.SOCKS,
          InetSocketAddress.createUnresolved("127.0.0.1", 9050)
      );
    } else {
      String host = sharedPrefs.getString(NETWORK.PROXY_HOST, SETTINGS_DEFAULT.NETWORK.PROXY_HOST);
      int port = sharedPrefs.getInt(NETWORK.PROXY_PORT, SETTINGS_DEFAULT.NETWORK.PROXY_PORT);
      this.proxy = new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(host, port));
    }
  }

  @Override
  protected HttpURLConnection createConnection(URL url) throws IOException {
      // source: https://gitlab.com/guardianproject/NetCipher/-/blob/master/netcipher-volley/src/info/guardianproject/netcipher/client/StrongHurlStack.java

      HttpURLConnection result;
      if (proxy == null) {
        result = (HttpURLConnection) url.openConnection();
      } else {
        result = (HttpURLConnection) url.openConnection(proxy);
      }

      // following from original HurlStack
      // Workaround for the M release HttpURLConnection not observing the
      // HttpURLConnection.setFollowRedirects() property.
      // https://code.google.com/p/android/issues/detail?id=194495
      result.setInstanceFollowRedirects(HttpURLConnection.getFollowRedirects());

      return(result);
  }
}
