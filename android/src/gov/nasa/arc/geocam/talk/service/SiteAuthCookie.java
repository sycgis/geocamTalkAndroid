package gov.nasa.arc.geocam.talk.service;

import gov.nasa.arc.geocam.talk.R;
import gov.nasa.arc.geocam.talk.exception.AuthenticationFailedException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import roboguice.inject.InjectResource;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.inject.Inject;

public class SiteAuthCookie implements ISiteAuth {

	@InjectResource(R.string.url_server_root)
	String serverRootUrl;
	
	@InjectResource(R.string.url_relative_app)
	String appPath;

	private DefaultHttpClient httpClient;
	private Cookie sessionIdCookie;
	private Context context;
	
	@Inject
	public SiteAuthCookie(Context context) {
		this.context = context;
	}

	@Override
	public void setRoot(String siteRoot) {
		serverRootUrl = siteRoot;
	}

	@Override
	public int post(String relativePath, Map<String, String> params)
			throws AuthenticationFailedException, IOException, ClientProtocolException,
			InvalidParameterException {
		return post(relativePath, params, null);
	}

	@Override
	public int post(String relativePath, Map<String, String> params, byte[] audioBytes)
			throws AuthenticationFailedException, IOException, ClientProtocolException,
			InvalidParameterException {
		if (params == null) {
			throw new InvalidParameterException("Post parameters are required");
		}
		
		ensureAuthenticated();

		httpClient = new DefaultHttpClient();

		HttpParams httpParams = httpClient.getParams();
		HttpClientParams.setRedirecting(httpParams, false);

		HttpPost post = new HttpPost(this.serverRootUrl + "/" + appPath + "/" + relativePath);
		post.setParams(httpParams);

		HttpEntity httpEntity;
		
		if (audioBytes != null) {
			httpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

			for (String key : params.keySet()) {
				((MultipartEntity) httpEntity).addPart(key, new StringBody(params.get(key)));
			}
			if (audioBytes != null) {
				((MultipartEntity) httpEntity).addPart("audio", new ByteArrayBody(audioBytes,
						"audio/mpeg", "audio.mp4"));
			}
		} else {
			List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();

			for (String key : params.keySet()) {
				nameValuePairs.add(new BasicNameValuePair(key, params.get(key)));
			}

			httpEntity = new UrlEncodedFormEntity(nameValuePairs, HTTP.ASCII);
		}
		
		post.setEntity(httpEntity);

		httpClient.getCookieStore().addCookie(sessionIdCookie);
		// post.setHeader("Cookie", sessionIdCookie.toString());

		HttpResponse r = httpClient.execute(post);
		// TODO: check for redirect to login and call login if is the case

		return r.getStatusLine().getStatusCode();
	}

	@Override
	public String get(String relativePath, Map<String, String> params)
			throws AuthenticationFailedException, IOException, ClientProtocolException {
		ensureAuthenticated();
		httpClient = new DefaultHttpClient();

		HttpGet get = new HttpGet(this.serverRootUrl + "/" + appPath + "/" + relativePath);

		// TODO: add param parsing and query string construction as necessary

		httpClient.getCookieStore().addCookie(sessionIdCookie);
		// get.setHeader("Cookie", sessionIdCookie.toString());

		HttpResponse r = httpClient.execute(get);
		// TODO: check for redirect to login and call login if is the case

		InputStream content = r.getEntity().getContent();

		BufferedReader br = new BufferedReader(new InputStreamReader(content));
		StringBuilder sb = new StringBuilder();
		String line = null;

		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}

		br.close();
		return sb.toString();
	}

	private void ensureAuthenticated() throws AuthenticationFailedException,
			ClientProtocolException, IOException {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		String username = prefs.getString("webapp_username", null);
		String password = prefs.getString("webapp_password", null);

		if (username == null || password == null) {
			throw new AuthenticationFailedException("Username and/or password not set.");
		} else {
			Date now = new Date();
			if (sessionIdCookie == null || sessionIdCookie.isExpired(now)) {
				// we're not logged in (at least we think. Let's log in)
				login(username, password);
			}
		}
	}

	private void login(String username, String password) throws ClientProtocolException,
			IOException, AuthenticationFailedException {
		httpClient = new DefaultHttpClient();
		HttpParams params = httpClient.getParams();
		HttpClientParams.setRedirecting(params, false);

		HttpPost p = new HttpPost(serverRootUrl + "/accounts/login/");
		p.setParams(params);

		List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("username", username));
		nameValuePairs.add(new BasicNameValuePair("password", password));

		p.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.ASCII));

		HttpResponse r = httpClient.execute(p);
		if (302 == r.getStatusLine().getStatusCode()) {
			for (Cookie c : httpClient.getCookieStore().getCookies()) {
				if (c.getName().contains("sessionid")) {
					sessionIdCookie = c;
					return;
				}
			}
			throw new AuthenticationFailedException(
					"Session cookie was missing from server login response.");
		} else {
			throw new AuthenticationFailedException("Got unexpected response code from server: "
					+ r.getStatusLine().getStatusCode());
		}
	}
}
