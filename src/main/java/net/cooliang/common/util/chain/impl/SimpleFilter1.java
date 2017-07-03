package net.cooliang.common.util.chain.impl;

import net.cooliang.common.util.chain.Filter;
import net.cooliang.common.util.chain.FilterChain;
import net.cooliang.common.util.chain.FilterConfig;

public class SimpleFilter1 implements Filter {

	@Override
	public void doFilter(FilterConfig config, FilterChain chain) {
		System.out.println(getClass().getSimpleName());
		chain.doFilter(config);
	}

}
