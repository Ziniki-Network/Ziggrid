package org.ziggrid.utils.graphs;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.lambda.FuncR1;
import org.ziggrid.utils.lambda.Lambda;

public class Node<N> {
	N node;
	private Set<Link<N>> linksTo = new HashSet<Link<N>>();
	private Set<Link<N>> linksFrom = new HashSet<Link<N>>();
	private Set<Node<N>> span = new HashSet<Node<N>>();
	private FuncR1<N, Node<N>> getNode = new FuncR1<N, Node<N>>() {
		@Override
		public N apply(Node<N> n) {
			return n.node;
		}
	};

	public Node(N node) {
		if (node == null)
			throw new UtilException("Cannot add null node");
		this.node = node;
	}

	public void addLinkTo(Link<N> link)
	{
		linksTo.add(link);
	}
	
	public void addLinkFrom(Link<N> link)
	{
		linksFrom.add(link);
		try
		{
			span.addAll(link.to.span);
			span.add(link.to);
		}
		catch (ConcurrentModificationException ex)
		{
			ex.printStackTrace();
			System.out.println("This was while trying to link from " + link + " to " + this);
			System.out.println("That link was from " + link.from + " to " + link.to);
			System.out.println("This is quite possibly some kind of circular dependency");
		}
	}
	
	public Set<N> span()
	{
		return Lambda.map(getNode, span);
	}
	
	
	@Override
	public int hashCode() {
		return node.hashCode();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Node && ((Node<N>)obj).node.equals(node);
	}

	// Is this a set or a bag (in the general case?)
	public Set<Link<N>> linksTo() {
		return linksTo;
	}
	
	@Override
	public String toString() {
		return node.toString();
	}

	public Set<Link<N>> linksFrom() {
		return linksFrom ;
	}

	public N getEntry() {
		return node;
	}

	void setEntry(N br) {
		node = br;
	}
}
