package org.ziggrid.utils.graphs;

import org.ziggrid.utils.lambda.FuncR1;

public class Link<N> {
	Node<N> from;
	Node<N> to;
	public FuncR1<Node<N>, Link<N>> extractFrom = new FuncR1<Node<N>, Link<N>>() {
		@Override
		public Node<N> apply(Link<N> arg1) {
			return arg1.from;
		}
	};
	public FuncR1<Node<N>, Link<N>> extractTo = new FuncR1<Node<N>, Link<N>>() {
		@Override
		public Node<N> apply(Link<N> arg1) {
			return arg1.to;
		}
	};
	
	public Link(Node<N> f, Node<N> t) {
		from = f;
		to = t;
	}
	
	@Override
	public int hashCode() {
		return from.hashCode() ^ to.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Link<?>))
			return false;
		@SuppressWarnings("unchecked")
		Link<N> other = (Link<N>)obj;
		return other.from.node.equals(from.node) && other.to.node.equals(to.node);
	}
	@Override
	public String toString() {
		return "Link[" + from + " => " + to +"]";
	}

	public N getTo() {
		return to.node;
	}

	public Node<N> getFromNode() {
		return from;
	}
}
