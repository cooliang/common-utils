package net.cooliang.common.utils.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.CodingErrorAction;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpClientUtils {

	private static final int MAX_TOTAL_CONNECTIONS = 2;
	private static final int MAX_ROUTE_CONNECTIONS = 2;
	private static final int CONNECT_TIMEOUT = 10000;
	
	private static PoolingHttpClientConnectionManager connManager = null;

	static {
		try {
			SSLContext sslContext = SSLContexts.custom().useTLS().build();
			X509TrustManager tm = new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			sslContext.init(null, new TrustManager[] { tm }, null);
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", new SSLConnectionSocketFactory(sslContext)).build();
			connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			// Create socket configuration
			SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
			connManager.setDefaultSocketConfig(socketConfig);
			// Create message constraints
			MessageConstraints messageConstraints = MessageConstraints.custom().setMaxHeaderCount(200)
					.setMaxLineLength(2000).build();
			// Create connection configuration
			ConnectionConfig connectionConfig = ConnectionConfig.custom()
					.setMalformedInputAction(CodingErrorAction.IGNORE)
					.setUnmappableInputAction(CodingErrorAction.IGNORE).setCharset(Consts.UTF_8)
					.setMessageConstraints(messageConstraints).build();
			connManager.setDefaultConnectionConfig(connectionConfig);
			connManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
			connManager.setDefaultMaxPerRoute(MAX_ROUTE_CONNECTIONS);
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private static CloseableHttpClient getDefaultHttpClient() {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT)
				.setConnectionRequestTimeout(CONNECT_TIMEOUT).setSocketTimeout(CONNECT_TIMEOUT).build();
		return HttpClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(requestConfig).build();
	}

	public static String doGet(String url, Map<String, String> headers, Map<String, String> params) {
		return doGet(url, headers, params, "UTF-8");
	}

	public static String doGet(String url, Map<String, String> headers, Map<String, String> params, String encoding) {
		if (StringUtils.isBlank(url)) {
			throw new IllegalAccessError("url can not null");
		}
		if (StringUtils.isBlank(encoding)) {
			throw new IllegalAccessError("encoding can not null");
		}
		CloseableHttpClient httpClient = getDefaultHttpClient();
		if (MapUtils.isNotEmpty(params)) {
			String[] paramArr = new String[params.size()];
			int count = 0;
			for (Entry<String, String> param : params.entrySet()) {
				String value = "";
				try {
					value = URLEncoder.encode(param.getValue(), encoding);
				} catch (UnsupportedEncodingException e) {
					value = param.getValue();
				}
				paramArr[count++] = param.getKey() + "=" + value;
			}
			if (url.indexOf("?") != -1) {
				url = url + "?" + StringUtils.join(paramArr, "&");
			} else {
				url = url + "&" + StringUtils.join(paramArr, "&");
			}
		}
		HttpGet get = new HttpGet(url);
		if (MapUtils.isNotEmpty(headers)) {
			for (Entry<String, String> header : headers.entrySet()) {
				get.addHeader(header.getKey(), header.getValue());
			}
		}
		try {
			CloseableHttpResponse response = httpClient.execute(get);
			StatusLine statusLine = response.getStatusLine();
			if (null == statusLine) {
				throw new RuntimeException("Http Request Error, Status Line is null");
			}
			if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
				throw new RuntimeException("Http Request Error, Http Code is " + statusLine.getStatusCode());
			}
			HttpEntity entity = response.getEntity();
			return EntityUtils.toString(entity, encoding);
		} catch (IOException e) {
			throw new RuntimeException("Http Request Error", e);
		} finally {
			if (null != get) {
				get.releaseConnection();
			}
		}
	}

	public static String doPostByForm(String url, Map<String, String> headers, Map<String, String> params, String encoding) {
		if (StringUtils.isBlank(url)) {
			throw new IllegalAccessError("url can not null");
		}
		if (StringUtils.isBlank(encoding)) {
			throw new IllegalAccessError("encoding can not null");
		}
		CloseableHttpClient httpClient = getDefaultHttpClient();
		HttpPost post = new HttpPost(url);
		if (MapUtils.isNotEmpty(headers)) {
			for (Entry<String, String> header : headers.entrySet()) {
				post.addHeader(header.getKey(), header.getValue());
			}
		}
		if (MapUtils.isNotEmpty(params)) {
			List<NameValuePair> list = new ArrayList<NameValuePair>(params.size());
			NameValuePair pair = null;
			for (Entry<String, String> param : params.entrySet()) {
				pair = new BasicNameValuePair(param.getKey(), param.getValue());
				list.add(pair);
			}
			post.setEntity(new UrlEncodedFormEntity(list, Consts.UTF_8));
		}
		try {
			CloseableHttpResponse response = httpClient.execute(post);
			StatusLine statusLine = response.getStatusLine();
			if (null == statusLine) {
				throw new RuntimeException("Http Request Error, Status Line is null");
			}
			if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
				throw new RuntimeException("Http Request Error, Http Code is " + statusLine.getStatusCode());
			}
			HttpEntity entity = response.getEntity();
			return EntityUtils.toString(entity, encoding);
		} catch (IOException e) {
			throw new RuntimeException("Http Request Error", e);
		} finally {
			if (null != post) {
				post.releaseConnection();
			}
		}
	}

	public static String doPostByJson(String url, Map<String, String> headers, Map<String, String> params, String encoding) {
		if (StringUtils.isBlank(url)) {
			throw new IllegalAccessError("url can not null");
		}
		if (StringUtils.isBlank(encoding)) {
			throw new IllegalAccessError("encoding can not null");
		}
		CloseableHttpClient httpClient = getDefaultHttpClient();
		HttpPost post = new HttpPost(url);
		if (MapUtils.isNotEmpty(headers)) {
			for (Entry<String, String> header : headers.entrySet()) {
				post.addHeader(header.getKey(), header.getValue());
			}
		}
		if (MapUtils.isNotEmpty(params)) {
			List<NameValuePair> list = new ArrayList<NameValuePair>(params.size());
			NameValuePair pair = null;
			for (Entry<String, String> param : params.entrySet()) {
				pair = new BasicNameValuePair(param.getKey(), param.getValue());
				list.add(pair);
			}
			post.setEntity(new UrlEncodedFormEntity(list, Consts.UTF_8));
		}
		try {
			CloseableHttpResponse response = httpClient.execute(post);
			StatusLine statusLine = response.getStatusLine();
			if (null == statusLine) {
				throw new RuntimeException("Http Request Error, Status Line is null");
			}
			if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
				throw new RuntimeException("Http Request Error, Http Code is " + statusLine.getStatusCode());
			}
			HttpEntity entity = response.getEntity();
			return EntityUtils.toString(entity, encoding);
		} catch (IOException e) {
			throw new RuntimeException("Http Request Error", e);
		} finally {
			if (null != post) {
				post.releaseConnection();
			}
		}
	}
}
