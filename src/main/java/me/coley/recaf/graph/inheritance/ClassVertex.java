package me.coley.recaf.graph.inheritance;

import me.coley.recaf.graph.*;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.ClasspathUtil;
import org.objectweb.asm.ClassReader;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Graph vertex with a {@link org.objectweb.asm.ClassReader} as the data.
 *
 * @author Matt
 */
public class ClassVertex extends Vertex<ClassReader> {
	private final Hierarchy graph;
	private ClassReader clazz;

	/**
	 * Constructs a class vertex from the containing hierarchy and class reader.
	 *
	 * @param graph
	 * 		The containing hierarchy.
	 * @param clazz
	 * 		The vertex data.
	 */
	public ClassVertex(Hierarchy graph, ClassReader clazz) {
		this.clazz = clazz;
		this.graph = graph;
	}

	@Override
	public ClassReader getData() {
		return clazz;
	}

	@Override
	public void setData(ClassReader clazz) {
		this.clazz = clazz;
	}

	@Override
	public int hashCode() {
		return getData().getClassName().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if(other == null)
			throw new IllegalStateException();
		if(this == other)
			return true;
		if(other instanceof ClassVertex) {
			ClassVertex otherVertex = (ClassVertex) other;
			if(getData().getClassName().equals(otherVertex.getData().getClassName()))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return getData().getClassName();
	}

	@Override
	public Set<Edge<ClassReader>> getEdges() {
		// Get names of parents/children
		Stream<String> parents = graph.getParents(getData().getClassName());
		Stream<String> children = graph.getDescendants(getData().getClassName());
		// Get values of parents/children
		Stream<ClassReader> parentValues = getReadersFromNames(parents);
		Stream<ClassReader> childrenValues = getReadersFromNames(children);
		// Get edges of parents/children
		Stream<Edge<ClassReader>> parentEdges = parentValues.map(node -> {
			ClassVertex other = graph.getVertex(node.getClassName());
			if(other == null) {
				other = new ClassVertex(graph, node);
			}
			return new DirectedEdge<>(other, ClassVertex.this);
		});
		Stream<Edge<ClassReader>> childrenEdges = childrenValues.map(node -> {
			ClassVertex other = graph.getVertex(node.getClassName());
			return new DirectedEdge<>(ClassVertex.this, other);
		});
		// Concat edges and return as set.
		return Stream.concat(parentEdges, childrenEdges).collect(Collectors.toSet());
	}

	// ============================== UTILITY =================================== //

	/**
	 * @param names
	 * 		Stream of names of classes.
	 *
	 * @return Mapped stream where names are replaced with instances.
	 * If a name has no instance mapping, it is discarded.
	 */
	private Stream<ClassReader> getReadersFromNames(Stream<String> names) {
		return names.map(name -> {
			// Try loading from workspace
			ClassReader reader = graph.getWorkspace().getClassReader(name);
			if(reader != null)
				return reader;
			// Try loading from runtime
			return ClassUtil.fromRuntime(name);
		}).filter(node -> node != null);
	}
}