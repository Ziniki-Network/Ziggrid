package org.ziggrid.utils.graphs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.exceptions.CycleDetectedException;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.lambda.FuncR1;
import org.ziggrid.utils.lambda.Lambda;

public class DirectedAcyclicGraph<N> {
	private final HashSet<Node<N>> nodes = new HashSet<Node<N>>();
	private final HashSet<Link<N>> links = new HashSet<Link<N>>();
	private Comparator<N> spanSize = new Comparator<N>() {
		@Override
		public int compare(N arg0, N arg1) {
			Node<N> lhs = find(arg0);
			Node<N> rhs = find(arg1);
			return nodeSpanSize.compare(lhs,rhs);
		}
	};
	private Comparator<Node<N>> nodeSpanSize = new Comparator<Node<N>>() {
		@Override
		public int compare(Node<N> lhs, Node<N> rhs) {
			if (lhs.span().size() > rhs.span().size())
				return -1;
			else if (lhs.span().size() == rhs.span().size())
				return 0;
			else
				return 1;
		}
	};
	private FuncR1<Node<N>, N> findNode = new FuncR1<Node<N>, N>() {
		@Override
		public Node<N> apply(N n) {
			return find(n);
		}
	};

	public void newNode(N node) {
//		System.out.println("Adding " + node);
		if (nodes.contains(node))
			throw new UtilException("Cannot add the same node " + node + " twice");
		Node<N> n = new Node<N>(node);
		nodes.add(n);
	}
	
	public void ensure(N node) {
		if (nodes.contains(node))
			return;
		newNode(node);
	}
	
	public Set<N> nodes() {
		Set<N> ret = new HashSet<N>();
		for (Node<N> n : nodes)
			ret.add(n.getEntry());
		return ret;
	}


	public void link(N from, N to) {
		Node<N> f = find(from);
		Node<N> t = find(to);
		addLink(new Link<N>(f,t));
	}

	public boolean hasLink(N from, N to) {
		return hasLinkInternal(from, to) == null;
	}

	private Link<N> hasLinkInternal(N from, N to) {
		Node<N> f = find(from);
		Node<N> t = find(to);
		Link<N> l = new Link<N>(f, t);
		if (links.contains(l))
			return null;
		return l;
	}


	public void ensureLink(N from, N to) {
		Link<N> l = hasLinkInternal(from, to);
		if (l != null)
			addLink(l);
	}

	private void addLink(Link<N> link) {
		if (link.to.span().contains(link.from.node))
			throw new CycleDetectedException("Adding link from " + link.from.node + " to " + link.to.node + " creates a cycle");
		links.add(link);
		link.from.addLinkFrom(link);
		link.to.addLinkTo(link);
	}
	

	public Node<N> find(N n) {
		for (Node<N> ret : nodes)
			if (ret.node.equals(n))
				return ret;
		throw new UtilException("The node " + n + " was not in the graph");
	}

	public List<N> roots()
	{
		List<N> ret = new ArrayList<N>();
		for (Node<N> n : nodes)
		{
			if (n.linksTo().size() == 0)
				ret.add(n.node);
		}
		Collections.sort(ret, spanSize);
		return ret;
	}
	

	public void assertSpanning() {
		List<N> roots = roots();
		if (roots.size() != 1)
			throw new UtilException("The graph had more than one root");
	}


	public Iterable<N> children(N node) {
		Set<N> ret = new HashSet<N>();
		Node<N> root = find(node);
		for (Link<N> l : root.linksFrom())
			ret.add(l.to.node);
		return ret;
	}

	public Iterable<N> allChildren(N node) {
		Set<N> ret = new HashSet<N>();
		int cnt;
		ret.add(node);
		do
		{
			cnt = ret.size();
			Set<N> n2 = new HashSet<N>();
			for (N n : ret)
			{
				Node<N> root = find(n);
				for (Link<N> l : root.linksFrom())
					n2.add(l.to.node);
			}
			ret.addAll(n2);
		} while (ret.size() > cnt);
		ret.remove(node);
		return ret;
	}

	public void postOrderTraverse(NodeWalker<N> nodeWalker) {
		Set<Node<N>> done = new HashSet<Node<N>>();
		postOrder(nodeWalker, done, Lambda.map(findNode, roots()));
	}
	
	private void postOrder(NodeWalker<N> walker, Set<Node<N>> done, List<Node<N>> todo)
	{
		for (Node<N> n : todo)
		{
			if (done.contains(n))
				continue;
			done.add(n);
			if (n.linksFrom().size() > 0)
				postOrder(walker, done, CollectionUtils.setToList(Lambda.map(CollectionUtils.any(n.linksFrom()).extractTo, n.linksFrom()), nodeSpanSize));
			walker.present(n);
		}
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		Set<Node<N>> nodesDone = new HashSet<Node<N>>();
		Set<Link<N>> linksDone = new HashSet<Link<N>>();
		for (N root : roots())
			recurseOver(ret, nodesDone, linksDone, find(root));
		return ret.toString();
	}

	private void recurseOver(StringBuilder ret, Set<Node<N>> nodesDone,	Set<Link<N>> linksDone, Node<N> n) {
		if (nodesDone.contains(n))
			return;
		ret.append("Node " + n + " depends on:\n");
		Set<Node<N>> ineed = new HashSet<Node<N>>();
		for (Link<N> l : n.linksFrom())
		{
			ret.append("  " + l.to + "\n");
			ineed.add(l.to);
		}
		nodesDone.add(n);
		for (Node<N> child : ineed)
			recurseOver(ret, nodesDone, linksDone, child);
	}

	public void rename(Node<N> node, N entry) {
		Node<N> curr = find(entry);
		if (curr != null && curr != node)
			throw new UtilException("Cannot create duplicate node name " + entry);
		node.setEntry(entry);
	}

	public void clear() {
		nodes.clear();
		links.clear();
	}
}
