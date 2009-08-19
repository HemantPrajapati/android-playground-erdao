/*
 * Copyright (C) 2009 Huan Erdao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.erdao.PhotSpot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

/* Class for Getting the PhotoFeed */
public class PhotoFeedGetter {
	/* variables */
	private HttpClient httpClient_;
	private final int connection_Timeout = 10000;
	private final Context context_;
	int service_;
	private ArrayList<PhotoItem> photoItems_ = new ArrayList<PhotoItem>();
	public final static int CODE_HTTPERROR	= -1;
	public final static int CODE_NORESULT	= 0;
	public final static int CODE_OK			= 1;

	/* constructor */
	public PhotoFeedGetter(int svc, Context context){
		service_ = svc;
		context_ = context;
		final HttpParams httpParams = new BasicHttpParams();
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
		HttpConnectionParams.setConnectionTimeout(httpParams,connection_Timeout); 
		HttpConnectionParams.setSoTimeout(httpParams,connection_Timeout); 
		final SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		httpClient_ = new DefaultHttpClient(
				new ThreadSafeClientConnManager(httpParams, schemeRegistry),
				httpParams);
		
	}

	/* getFeed */
	public Integer getFeed(String uri){
		final HttpGet get = new HttpGet(uri);
		HttpEntity entity = null;
		HttpResponse response;
		StringBuilder strbuilder = null;
		InputStream is = null;
		try {
			response = httpClient_.execute(get);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				is = entity.getContent();
				InputStreamReader isr = new InputStreamReader(is,"UTF-8");
				BufferedReader reader = new BufferedReader(isr,1024);
				strbuilder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					strbuilder.append(line + "\n");
				}
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				if(is!=null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if( strbuilder == null )
			return CODE_HTTPERROR;

		String result = strbuilder.toString();
		JSONObject jsonobj;
		try {
			JSONArray array = null;
			jsonobj = new JSONObject(result);
			long feedcount = jsonobj.getLong("count");
			if(feedcount==0)
				return CODE_NORESULT;
			array = jsonobj.getJSONArray("photo");
			int count = array.length();
			for (int i = 0; i < count; i++) {
				if( photoItems_.size() >= 100 )	// max = 100 items
					break;
				JSONObject obj = array.getJSONObject(i);
				long id = 0;
				String title = null, thumb = null, photoUrl = null, owner= null;
				double lat = 0.0,lng = 0.0;
				id = obj.getLong("id");
				title = obj.getString("title");
				thumb = obj.getString("thumbUrl");
				photoUrl = obj.getString("photoUrl");
				lat = obj.getDouble("lat");
				lng = obj.getDouble("lng");
				owner = obj.getString("author");
				if(title == null || title.length() == 0) {
					title = context_.getString(R.string.no_title);
				}
				PhotoItem item =
					new PhotoItem(id,thumb,(int)(lat*1E6),(int)(lng*1E6),title,photoUrl,owner);
				photoItems_.add(item);
			}
			return CODE_OK; 
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return CODE_NORESULT;
	}
	
	/* getPhotoItemList */
	public ArrayList<PhotoItem> getPhotoItemList(){
		return photoItems_;
	}
}
