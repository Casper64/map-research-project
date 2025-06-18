/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 Casper Küthe <casperck64@gmail.com>
 * SPDX-License-Identifier: MIT
 *
 * The code in this file is largely inspired by
 * https://www.happycoders.eu/algorithms/red-black-tree-java/
 */

package com.casperswebsites.trees.redblack;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * RedBlack tree implementation with support for iterators and streams. <br>
 * <br>
 * A red black tree conform to the following properties:
 * <ol>
 *  <li>Every node is either red or black.</li>
 *  <li>The root is black.</li>
 *  <li>Every leaf (nil) is black.</li>
 *  <li>If a node is red then both of its children are black.</li>
 *  <li>For each node, all paths from the node to descendant leaves contain the same number of black nodes.</li>
 * </ol>
 *
 * @param <K>
 * @param <V>
 * @author Casper Küthe
 * @see Comparable
 * @see Iterable
 * @see Map
 */
public class RedBlackTree<K extends Comparable<K>, V>
    extends AbstractMap<K, V>
    implements Iterable<V> {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    /**
     * the number of values
     */
    private int size = 0;

    /**
     * The comparator used to maintain order in this tree map, or
     * null if it uses the natural ordering of its keys.
     */
    private final Comparator<? super K> comparator;
    /**
     * root of the tree
     */
    private Node root;
    /**
     * Node with the smallest value
     */
    private Node smallestNode;

    /**
     * Creates a red-black tree using the natural ordering of the keys
     */
    public RedBlackTree() {
        this.comparator = K::compareTo;
    }

    /**
     * Creates a red-black tree using a custom comparator to determine ordering
     */
    public RedBlackTree(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * @return the number of values in the tree
     */
    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public Comparator<? super K> comparator() {
        return comparator;
    }

    // ====================================
    //      Utility functions
    // ====================================

    @Override
    public String toString() {
        return "RedBlack tree (Values: %d)".formatted(size);
    }

    /**
     * @return a string representation of the values in the tree
     */
    public String valuesString() {
        return stream().map(Object::toString).collect(Collectors.joining(" --> "));
    }

    // ============================================
    //        RedBlack tree core functionality
    // ============================================

    /**
     * Return the value for the specified key
     *
     * @param key
     * @return the value for the specified key, or null if the key does not exist.
     * @throws NullPointerException if the key is null
     */
    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        if (root == null) return null;
        Objects.requireNonNull(key);

        return root.search((K) key);
    }

    /**
     * Insert a key-value pair in the tree
     *
     * @param key
     * @param value
     * @param replaceOld whether to replace the old value if {@code key} already exists in the tree
     * @return the old value associated with {@code key}
     */
    private V putInternal(K key, V value, boolean replaceOld) {
        Node node = new Node(key, value, RED);
        if (root == null) {
            root = node;
            root.colour = BLACK;
            size = 1;
            smallestNode = node;
            return null;
        }

        // find node where to insert
        Node target = root;
        for (Node current = root; current != null;) {
            if (replaceOld && comparator.compare(key, current.key) == 0) {
                // replace the value of the node if the key is the same
                V oldValue = current.value;
                current.value = value;
                return oldValue;
            }

            target = current;
            if (comparator.compare(key, current.key) < 0) current = current.left;
            else current = current.right;
        }
        // insert node in the parent
        node.parent = target;

        if (comparator.compare(key, target.key) < 0) target.left = node;
        else if (comparator.compare(key, target.key) > 0) target.right = node;

        if (comparator.compare(key, smallestNode.key) < 0) smallestNode = node;

        insertFix(node);

        size++;
        return null;
    }

    /**
     * Fix the rules of the red-black tree after an insert
     */
    private void insertFix(Node node) {
        // case 1 don't need to do anything if the parent is null (the root)
        // case 2: if the parent is red rule 4 is violated, because inserted nodes
        // are always red
        while (node.parent != null && node.parent.colour == RED) {
            Node parent = node.parent;
            Node grandParent = parent.parent;

            boolean inLeftSubtree = grandParent.left == parent;
            Node uncle = inLeftSubtree ? grandParent.right : grandParent.left;

            if (uncle != null && uncle.colour == RED) {
                // case 3: uncle is red: recolour the grandparent and its children (rule 4)
                grandParent.recolourRed();
                node = grandParent;
            } else {
                // case 4: uncle is black (nil) and an "inner child"
                // i.e. the current node, its parent and grandparent
                // form a triangle around the uncle.
                // balance the tree by performing a RL or RL rotation depending on which side
                // the uncle is relative to the current node
                if (inLeftSubtree && node == parent.right) {
                    rotateLeft(parent);
                    parent = node;
                } else if (!inLeftSubtree && node == parent.left) {
                    rotateRight(parent);
                    parent = node;
                }
                // at this point the node is always an "outer child"
                // i.e. the current node and its parent and grandparent form a straight line
                // at this point both the node and its parent are red and its grandparent is black
                // if we rotate the grandparent and recolour it and the nodes parent the tree is fixed
                if (inLeftSubtree) rotateRight(grandParent);
                else rotateLeft(grandParent);

                parent.colour = BLACK;
                grandParent.colour = RED;
                // tree is fixed at this point and we don't need to check again
                break;
            }
        }
        // enforce rule 2. always make the root colour black
        root.colour = BLACK;
    }

    // Delete code was copied and adjusted from an recursive to an iterative approach from
    // https://www.happycoders.eu/algorithms/red-black-tree-java/

    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (root == null) return null;
        Objects.requireNonNull(key);

        Node node = root;
        while (node != null && !node.key.equals(key)) {
            if (comparator.compare((K) key, node.key) < 0) {
                node = node.left;
            } else {
                node = node.right;
            }
        }

        // node does not exist
        if (node == null) return null;

        V oldValue = node.value;
        Node movedUpNode;
        boolean deletedNodeColor;

        // node has two children, replace it with its inorder successor
        if (node.left != null && node.right != null) {
            Node ios = node.right;
            while (ios.left != null) {
                ios = ios.left;
            }

            node.key = ios.key;
            node.value = ios.value;
            node = ios;
        }

        // at this point the node has at most one child
        deletedNodeColor = node.colour;

        if (node.left != null) {
            movedUpNode = node.left;
            replaceParentsChild(node.parent, node, node.left);
        } else if (node.right != null) {
            movedUpNode = node.right;
            replaceParentsChild(node.parent, node, node.right);
        } else {
            // node has no children
            // we need a temporary NIL node for fixing, if the node was black
            movedUpNode = deletedNodeColor == BLACK ? new NilNode() : null;
            replaceParentsChild(node.parent, node, movedUpNode);
        }

        if (deletedNodeColor == BLACK) {
            // fix tree if delete node was black (violates the black height property)
            deleteFix(movedUpNode);

            if (movedUpNode != null && movedUpNode.getClass() == NilNode.class) {
                replaceParentsChild(movedUpNode.parent, movedUpNode, null);
            }
        }

        size--;
        return oldValue;
    }

    /**
     * Fix the red-black tree properties after deletion
     */
    private void deleteFix(Node node) {
        while (node != root) {
            Node parent = node.parent;
            Node sibling = (node == parent.left) ? parent.right : parent.left;

            // Case 1: red sibling: recolor and rotate to make sibling black
            if (sibling.colour == RED) {
                sibling.colour = BLACK;
                parent.colour = RED;

                if (node == parent.left) {
                    rotateLeft(parent);
                    sibling = parent.right;
                } else {
                    rotateRight(parent);
                    sibling = parent.left;
                }
            }

            // case 3-6: black sibling
            boolean siblingLeftBlack = (sibling.left == null || sibling.left.colour == BLACK);
            boolean siblingRightBlack = (sibling.right == null || sibling.right.colour == BLACK);

            if (siblingLeftBlack && siblingRightBlack) {
                sibling.colour = RED;

                if (parent.colour == RED) {
                    // case 3: Black sibling with two black children + red parent
                    parent.colour = BLACK;
                    // at this point the tree is fixed
                    break;
                } else {
                    // case 4: Black sibling with two black children + black parent
                    node = parent;
                }
            } else {
                boolean nodeIsLeftChild = (node == parent.left);

                // case 5: black sibling with red inner child and black outer child
                if (nodeIsLeftChild && siblingRightBlack) {
                    sibling.left.colour = BLACK;
                    sibling.colour = RED;
                    rotateRight(sibling);
                    sibling = parent.right;
                } else if (!nodeIsLeftChild && siblingLeftBlack) {
                    sibling.right.colour = BLACK;
                    sibling.colour = RED;
                    rotateLeft(sibling);
                    sibling = parent.left;
                }

                // case 6: black sibling with red outer child
                sibling.colour = parent.colour;
                parent.colour = BLACK;
                if (nodeIsLeftChild) {
                    sibling.right.colour = BLACK;
                    rotateLeft(parent);
                } else {
                    sibling.left.colour = BLACK;
                    rotateRight(parent);
                }
                // tree is fixed
                break;
            }
        }
    }

    /**
     * Replace a child of a parent node
     */
    private void replaceParentsChild(Node parent, Node oldChild, Node newChild) {
        if (parent == null) {
            root = newChild;
        } else if (parent.left == oldChild) {
            parent.left = newChild;
        } else if (parent.right == oldChild) {
            parent.right = newChild;
        } else {
            throw new IllegalStateException("Node is not a child of its parent");
        }

        if (newChild != null) {
            newChild.parent = parent;
        }
    }

    /**
     * Internal node in the tree
     */
    private class Node {

        K key;
        V value;
        Node parent, left, right;
        boolean colour; // color of parent link

        public Node(K key, V value, boolean color) {
            this.key = key;
            this.value = value;
            this.colour = color;
        }

        /**
         * Make the node itself red and its children black (rule 4)
         */
        public void recolourRed() {
            this.colour = RED;
            if (left != null) left.colour = BLACK;
            if (right != null) right.colour = BLACK;
        }

        public V search(K key) {
            int res = comparator.compare(key, this.key);
            if (res < 0) {
                if (left == null) return null;
                else return left.search(key);
            } else if (res > 0) {
                if (right == null) return null;
                else return right.search(key);
            } else {
                return value;
            }
        }

        public Node findNode(K key) {
            int res = comparator.compare(key, this.key);
            if (res < 0) {
                if (left == null) return null;
                else return left.findNode(key);
            } else if (res > 0) {
                if (right == null) return null;
                else return right.findNode(key);
            } else {
                return this;
            }
        }
    }

    /**
     * nil node is used to for fixing the tree during a deletion
     */
    private class NilNode extends Node {

        private NilNode() {
            super(null, null, BLACK);
        }
    }

    // Left rotate
    private void rotateLeft(Node x) {
        Node y = x.right;
        x.right = y.left;

        if (y.left != null) y.left.parent = x;
        y.parent = x.parent;

        if (x.parent == null) root = y;
        else if (x == x.parent.left) x.parent.left = y;
        else x.parent.right = y;

        y.left = x;
        x.parent = y;
    }

    // Right rotate
    private void rotateRight(Node x) {
        Node y = x.left;
        x.left = y.right;

        if (y.right != null) y.right.parent = x;
        y.parent = x.parent;

        if (x.parent == null) root = y;
        else if (x == x.parent.right) x.parent.right = y;
        else x.parent.left = y;

        y.right = x;
        x.parent = y;
    }

    // =======================================
    //      Abstract Map implementations
    // =======================================

    @Override
    public boolean containsValue(Object value) {
        if (root == null) return false;
        Objects.requireNonNull(value);

        for (V val : this) {
            if (val.equals(value)) return true;
        }

        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        if (root == null) return false;
        Objects.requireNonNull(key);

        return get(key) != null;
    }

    @Override
    public V put(K key, V value) {
        return putInternal(key, value, true);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putInternal(key, value, false);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        map.forEach(this::put);
    }

    @Override
    public void clear() {
        size = 0;
        smallestNode = null;
        root = null;
    }

    private EntrySet entrySet;
    private KeySet keySet;
    private Collection<V> values;

    @Override
    public Set<Entry<K, V>> entrySet() {
        EntrySet es = entrySet;
        if (es == null) {
            entrySet = new EntrySet();
            es = entrySet;
        }
        return es;
    }

    @Override
    public Set<K> keySet() {
        KeySet ks = keySet;
        if (ks == null) {
            keySet = new KeySet();
            ks = keySet;
        }
        return ks;
    }

    @Override
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    @Override
    public int hashCode() {
        // TODO: will this overflow in an integer??
        return entryStream().mapToInt(Entry::hashCode).reduce(0, Integer::sum);
    }

    private class Values extends AbstractCollection<V> {

        @Override
        public Iterator<V> iterator() {
            return RedBlackTree.this.iterator();
        }

        @Override
        public int size() {
            return RedBlackTree.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return RedBlackTree.this.containsValue(o);
        }

        @Override
        public boolean remove(Object o) {
            return RedBlackTree.this.remove(o) != null;
        }

        @Override
        public void clear() {
            RedBlackTree.this.clear();
        }

        @Override
        public Spliterator<V> spliterator() {
            return RedBlackTree.this.spliterator();
        }
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return RedBlackTree.this.entryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) return false;

            V val = RedBlackTree.this.get(entry.getKey());
            return val != null && val.equals(entry.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) return false;
            return RedBlackTree.this.remove(entry.getKey(), entry.getValue());
        }

        @Override
        public int size() {
            return RedBlackTree.this.size();
        }

        @Override
        public void clear() {
            RedBlackTree.this.clear();
        }

        @Override
        public Spliterator<Entry<K, V>> spliterator() {
            return RedBlackTree.this.entrySpliterator();
        }
    }

    private class KeySet extends AbstractSet<K> {

        @Override
        public Iterator<K> iterator() {
            return RedBlackTree.this.keyIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) return false;

            V val = RedBlackTree.this.get(entry.getKey());
            return val != null && val.equals(entry.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) return false;
            return RedBlackTree.this.remove(entry.getKey(), entry.getValue());
        }

        @Override
        public int size() {
            return RedBlackTree.this.size();
        }

        @Override
        public void clear() {
            RedBlackTree.this.clear();
        }

        @Override
        public Spliterator<K> spliterator() {
            return RedBlackTree.this.keySpliterator();
        }
    }

    // =============================================
    //      Iterator and Stream implementations
    // =============================================

    private <T> Iterator<T> basePartialIterator(
        K from,
        K to,
        BiFunction<Node, K, BaseIterator<T>> factory
    ) {
        if (root == null) return Collections.emptyIterator();
        if (
            from != null && to != null && comparator.compare(from, to) > 0
        ) return Collections.emptyIterator();

        // Node fromNode = from == null ? smallestNode : root.findNode(from);

        BaseIterator<T> it = factory.apply(root, from);
        if (to != null) it.setStopKey(to);
        return it;
    }

    @Override
    public Iterator<V> iterator() {
        return new ValueIterator(root, smallestNode.key);
    }

    /**
     * Returns an iterator that starts at {@code from} and stops when the next {@code key > to}
     *
     * @param from the start key (inclusive) (can be {@code null} to begin at the start)
     * @param to   the (exclusive) end key (can be {@code null} to stop at the end)
     * @return an iterator of the values
     */
    public Iterator<V> iterator(K from, K to) {
        return basePartialIterator(from, to, ValueIterator::new);
    }

    /**
     * @return an iterator of the keys of the tree
     */
    public Iterator<K> keyIterator() {
        return new KeyIterator(root, smallestNode.key);
    }

    /**
     * Returns an iterator that starts at {@code from} and stops when the next {@code key > to}
     *
     * @param from the start key (can be {@code null} to begin at the start)
     * @param to   the (exclusive) end key (can be {@code null} to stop at the end)
     * @return an iterator of the keys
     */
    public Iterator<K> keyIterator(K from, K to) {
        return basePartialIterator(from, to, KeyIterator::new);
    }

    /**
     * @return an iterator of the key and value pairs in the tree
     */
    public Iterator<Map.Entry<K, V>> entryIterator() {
        return new EntryIterator(root, smallestNode.key);
    }

    /**
     * Returns an iterator that starts at {@code from} and stops when the next {@code key > to}
     *
     * @param from the start key (can be {@code null} to begin at the start)
     * @param to   the (inclusive) end key (can be {@code null} to stop at the end)
     * @return an iterator of the values
     */
    public Iterator<Map.Entry<K, V>> entryIterator(K from, K to) {
        return basePartialIterator(from, to, EntryIterator::new);
    }

    @Override
    public Spliterator<V> spliterator() {
        return new ValueSpliterator(root, size);
    }

    /**
     * @return a spliterator of the keys
     */
    public Spliterator<K> keySpliterator() {
        return new KeySpliterator(root, size);
    }

    /**
     * @return a spliterator of the keys
     */
    public Spliterator<Map.Entry<K, V>> entrySpliterator() {
        return new EntrySpliterator(root, size);
    }

    /**
     * @return a sequential stream with the tree's values
     */
    public Stream<V> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Returns a stream that starts at {@code from} and stops when the next {@code key > to}
     *
     * @param from the start key (can be {@code null} to begin at the start)
     * @param to   the (inclusive) end key (can be {@code null} to stop at the end)
     * @return a stream of the values
     */
    public Stream<V> stream(K from, K to) {
        Iterator<V> it = iterator(from, to);
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                it,
                Spliterator.ORDERED |
                Spliterator.SORTED |
                Spliterator.NONNULL |
                Spliterator.DISTINCT
            ),
            false
        );
    }

    /**
     * @return a stream of the keys in the tree
     */
    public Stream<K> keyStream() {
        return StreamSupport.stream(keySpliterator(), false);
    }

    /**
     * Returns a stream that starts at {@code from} and stops when the next {@code key > to}
     *
     * @param from the start key (can be {@code null} to begin at the start)
     * @param to   the (inclusive) end key (can be {@code null} to stop at the end)
     * @return a stream of the keys
     */
    public Stream<K> keyStream(K from, K to) {
        Iterator<K> it = keyIterator(from, to);
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                it,
                Spliterator.ORDERED |
                Spliterator.SORTED |
                Spliterator.NONNULL |
                Spliterator.DISTINCT
            ),
            false
        );
    }

    /**
     * @return a sequential stream with the tree's entries
     */
    public Stream<Map.Entry<K, V>> entryStream() {
        Spliterator<Map.Entry<K, V>> split = new EntrySpliterator(root, size);
        return StreamSupport.stream(split, false);
    }

    /**
     * Returns a stream that starts at {@code from} and stops when the next {@code key > to}
     *
     * @param from the start key (can be {@code null} to begin at the start)
     * @param to   the (inclusive) end key (can be {@code null} to stop at the end)
     * @return a stream of the entries
     */
    public Stream<Map.Entry<K, V>> entryStream(K from, K to) {
        Iterator<Map.Entry<K, V>> it = entryIterator(from, to);
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                it,
                Spliterator.ORDERED |
                Spliterator.SORTED |
                Spliterator.NONNULL |
                Spliterator.DISTINCT
            ),
            false
        );
    }

    /**
     * @return a parallel stream with the tree's values
     */
    public Stream<V> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    /**
     * @return a parallel stream with the tree's values
     */
    public Stream<K> parallelKeyStream() {
        return StreamSupport.stream(keySpliterator(), true);
    }

    /**
     * @return a parallel stream with the tree's values
     */
    public Stream<Map.Entry<K, V>> parallelEntryStream() {
        return StreamSupport.stream(entrySpliterator(), true);
    }

    private abstract class BaseIterator<T> implements Iterator<T> {

        private Stack<Node> stack = new Stack<>();
        private K stopKey = null;
        private K startKey = null;

        protected Node currentNode;

        public BaseIterator(Node startNode, K startKey) {
            this.startKey = startKey;
            this.currentNode = startNode;
            pushLeft(startNode);
        }

        public void setStopKey(K stopKey) {
            this.stopKey = stopKey;
        }

        @Override
        public boolean hasNext() {
            if (stack.isEmpty()) return false;
            if (stopKey != null) {
                Node nextNode = stack.peek();
                return comparator.compare(nextNode.key, stopKey) <= 0;
            }

            return true;
        }

        private void pushLeft(Node node) {
            while (
                node != null && (startKey == null || comparator.compare(node.key, startKey) >= 0)
            ) {
                stack.push(node);
                node = node.left;
            }
        }

        @Override
        public T next() {
            currentNode = stack.pop();
            T result = get();
            if (currentNode.right != null) {
                pushLeft(currentNode.right);
            }
            return result;
        }

        protected abstract T get();
    }

    private class ValueIterator extends BaseIterator<V> {

        public ValueIterator(Node startNode, K startKey) {
            super(startNode, startKey);
        }

        @Override
        protected V get() {
            return currentNode.value;
        }
    }

    private class KeyIterator extends BaseIterator<K> {

        public KeyIterator(Node startNode, K startKey) {
            super(startNode, startKey);
        }

        @Override
        protected K get() {
            return currentNode.key;
        }
    }

    private class EntryIterator extends BaseIterator<Map.Entry<K, V>> {

        public EntryIterator(Node startNode, K startKey) {
            super(startNode, startKey);
        }

        @Override
        protected Map.Entry<K, V> get() {
            return new AbstractMap.SimpleEntry<>(currentNode.key, currentNode.value);
        }
    }

    /**
     * Base {@link Spliterator} class
     */
    private abstract class BaseSpliterator<T> implements Spliterator<T> {

        private Stack<Node> stack = new Stack<>();

        protected Node currentNode;
        protected int remaining;

        private final BiFunction<Node, Integer, BaseSpliterator<T>> factory;

        public BaseSpliterator(
            Node startNode,
            int size,
            BiFunction<Node, Integer, BaseSpliterator<T>> factory
        ) {
            this.currentNode = startNode;
            this.remaining = size;
            this.factory = factory;
            pushLeft(startNode);
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (remaining <= 0) return false;

            currentNode = stack.pop();
            action.accept(get());
            if (currentNode.right != null) {
                pushLeft(currentNode.right);
            }
            remaining--;
            return true;
        }

        @Override
        public Spliterator<T> trySplit() {
            if (remaining < 1000) return null;

            Node oldNode = currentNode;

            int splitSize = remaining / 2;
            int toSkip = splitSize;
            int total = 0;
            while (toSkip-- > 0) {
                currentNode = stack.pop();
                pushLeft(currentNode.right);
            }

            BaseSpliterator<T> split = factory.apply(oldNode, splitSize);
            remaining -= total;
            return split;
        }

        private void pushLeft(Node node) {
            // spliterator always processes the whole range,
            // so we don't need to check for a start key
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }

        @Override
        public long estimateSize() {
            return remaining;
        }

        @Override
        public int characteristics() {
            return ORDERED | SORTED | SIZED | NONNULL | DISTINCT;
        }

        @Override
        public Comparator<? super T> getComparator() {
            // return null indicating that the values are already "naturally ordered"
            return null;
        }

        protected abstract T get();
    }

    private class ValueSpliterator extends BaseSpliterator<V> {

        private ValueSpliterator(Node node, int size) {
            super(node, size, (startNode, total) ->
                new ValueSpliterator((RedBlackTree<K, V>.Node) startNode, total)
            );
        }

        @Override
        protected V get() {
            return currentNode.value;
        }
    }

    private class KeySpliterator extends BaseSpliterator<K> {

        private KeySpliterator(Node node, int size) {
            super(node, size, (startNode, total) ->
                new KeySpliterator((RedBlackTree<K, V>.Node) startNode, total)
            );
        }

        @Override
        protected K get() {
            return currentNode.key;
        }
    }

    private class EntrySpliterator extends BaseSpliterator<Entry<K, V>> {

        private EntrySpliterator(Node node, int size) {
            super(node, size, (startNode, total) ->
                new EntrySpliterator((RedBlackTree<K, V>.Node) startNode, total)
            );
        }

        @Override
        protected Map.Entry<K, V> get() {
            return Map.entry(currentNode.key, currentNode.value);
        }
    }
}
