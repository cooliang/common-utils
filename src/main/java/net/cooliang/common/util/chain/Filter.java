package net.cooliang.common.util.chain;

public interface Filter {

	void doFilter(FilterConfig config, FilterChain chain);

}
