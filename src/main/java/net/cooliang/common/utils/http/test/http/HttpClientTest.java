package net.cooliang.common.utils.http.test.http;

import net.cooliang.common.utils.http.HttpClientUtils;

public class HttpClientTest {
	public static void main(String[] args) {
		System.out.println(HttpClientUtils.doGet("https://www.alipay.com/", null, null, "utf-8"));
	}
}
