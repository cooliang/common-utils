package net.cooliang.common.util.chain;

import java.util.ArrayList;
import java.util.List;

public class CompositeFilter implements Filter {

	private List<Filter> filters = new ArrayList<Filter>();

	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	public void addFilter(Filter filter) {
		this.filters.add(filter);
	}

	@Override
	public void doFilter(FilterConfig config, FilterChain chain) {
		new VirtualFilterChain(chain, filters).doFilter(config);
	}

	private static class VirtualFilterChain implements FilterChain {

		private final FilterChain originalChain;
		private final List<Filter> additionalFilters;
		private int currentPosition = 0;

		public VirtualFilterChain(FilterChain chain, List<Filter> additionalFilters) {
			this.originalChain = chain;
			this.additionalFilters = additionalFilters;
		}

		@Override
		public void doFilter(FilterConfig config) {
			if (this.currentPosition == this.additionalFilters.size()) {
				this.originalChain.doFilter(config);
			} else {
				this.currentPosition++;
				Filter nextFilter = this.additionalFilters.get(this.currentPosition - 1);
				nextFilter.doFilter(config, this);
			}
		}

	}
}
