/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 Casper Küthe <casperck64@gmail.com>
 * SPDX-License-Identifier: MIT
 */

package com.casperswebsites.trees.bplustree;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * B+ tree implementation with support for iterators and streams. <br>
 * <br>
 * A B+ tree is a special kind of tree where all values (records) are stored in the leafs of the tree
 * and only the keys are stored in the nodes. <br>
 * In a B+ tree all the leafs are at the same level and a leave can store multiple values, this greatly reduces
 * the height of the tree. <br>
 * Each leaf holds a pointer to the next one so it's easy to iterate over the values of the tree. <br>
 * <br>
 *
 * @param <K> type of the keys which should extend the {@link Comparable} interface
 * @param <V> type for the values of the tree
 * @author Casper Küthe
 * @see Comparable
 * @see Iterable
 * @see Map
 */
public class BPlusTree<K extends Comparable<K>, V>
    extends AbstractMap<K, V>
    implements Iterable<V> {

    private final int order;
    protected final int minKeys;
    /**
     * the number of leafs
     */
    private int nLeafs = 0;
    /**
     * the number of values
     */
    private int size = 0;
    /**
     * the height of the tree
     */
    private int height = 0;

    /**
     * The comparator used to maintain order in this tree map, or
     * null if it uses the natural ordering of its keys.
     */
    private final Comparator<? super K> comparator;
    /**
     * root of the tree
     */
    protected Node<K, V> root;
    /**
     * the leaf with the smallest key (most left leaf)
     */
    protected NodeLeaf<K, V> firstLeaf;

    /**
     * @param order defines the order of the tree, maximum number of children per node
     * @throws IllegalArgumentException if {@code order < 3}
     */
    public BPlusTree(int order) {
        if (order < 3) throw new IllegalArgumentException("Order must be >= 3");
        this.order = order;
        this.comparator = K::compareTo;
        this.minKeys = (int) (Math.ceil(order / 2.0) - 1);
    }

    /**
     * @param order      defines the order of the tree, maximum number of children per node
     * @param comparator the comparator that should be used instead of the default comparator of the keys
     * @throws IllegalArgumentException if {@code order < 3}
     */
    public BPlusTree(int order, Comparator<? super K> comparator) {
        if (order < 3) throw new IllegalArgumentException("Order must be >= 3");
        this.order = order;
        this.comparator = comparator;
        this.minKeys = (int) (Math.ceil(order / 2.0) - 1);
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

    public int order() {
        return order;
    }

    public Comparator<? super K> comparator() {
        return comparator;
    }

    // ====================================
    //      Utility functions
    // ====================================
    //
    public Node<K, V> getRoot() {
        return root;
    }

    public NodeLeaf<K, V> getFirstLeaf() {
        return firstLeaf;
    }

    /**
     * @return the number of leaf nodes
     */
    public int getNLeafs() {
        return this.nLeafs;
    }

    /**
     * @return the height (depth) of the tree
     */
    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "B+ tree (Leafs: %d, Values: %d, Height: %d)".formatted(nLeafs, size, height);
    }

    /**
     * @return a string representation of the values in the tree
     */
    public String valuesString() {
        return stream().map(Object::toString).collect(Collectors.joining(" --> "));
    }

    /**
     * Prints the keys of the tree in a breadth-first traversal.
     */
    public void printNodeKeys() {
        if (root == null) return;

        Queue<Node<K, V>> queue = new LinkedList<>();

        queue.add(root);
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            System.out.print("| ");
            for (int i = 0; i < levelSize; i++) {
                Node<K, V> node = queue.poll();
                System.out.print(node.getKeys().toString());
                if (i != levelSize - 1) System.out.print(" | | ");

                if (node instanceof NodeInternal<K, V> internalNode) {
                    queue.addAll(internalNode.getChildren());
                }
            }
            System.out.println(" |");
        }
    }

    // =======================================
    //        B+ tree core functionality
    // =======================================

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
        if (root == null) {
            // first node
            firstLeaf = BPlusTree.this.createLeafNode(null);
            firstLeaf.insert(key, value);
            root = firstLeaf;

            size = 1;
            nLeafs = 1;
            height = 1;
            return value;
        }

        if (!replaceOld) {
            // return the old value if we don't want to replace the value and it already exists
            V val = get(key);
            if (val != null) return val;
        }

        SplitResult<K, V> splitResult = root.insert(key, value);

        if (splitResult.state == SplitState.INSERTED) size++;
        else if (splitResult.state == SplitState.REPLACED) return splitResult.replacedValue;
        else if (
            splitResult.state == SplitState.EMPTY || splitResult.state == SplitState.SPLIT_PREV_NODE
        ) {
            size++;
            nLeafs++;
        } else {
            splitRoot(splitResult);

            size++;
            nLeafs++;
            height++;
        }

        return null;
    }

    protected void splitRoot(SplitResult<K, V> splitResult) {
        // a leaf node will have always been split if the split results
        // propagates upwards to the root node and the key wasn't present before
        // we need to set a new root node.
        NodeInternal<K, V> newRoot = BPlusTree.this.createInternalNode(null);
        // add keys and children
        newRoot.getKeys().add(splitResult.key);
        newRoot.getChildren().add(splitResult.left);
        newRoot.getChildren().add(splitResult.right);
        // set parent pointers
        splitResult.right.setParent(newRoot);
        splitResult.left.setParent(newRoot);
        // set new root
        root = newRoot;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (size == 1) {
            if (get(key) == null) return null;

            V removedValue = firstLeaf.getValues().getFirst();

            // all nodes have been removed
            size = 0;
            root = null;
            firstLeaf = null;
            nLeafs = 0;
            return removedValue;
        }

        DeleteResult<K, V> deleteResult = root.delete((K) key, -1);

        // do nothing
        if (deleteResult.state == DeleteState.NOT_FOUND) return null;
        // a leaf node was merged
        else if (
            deleteResult.state == DeleteState.MERGED_LEAF ||
            deleteResult.state == DeleteState.MERGED
        ) nLeafs--;
        // we can now confirm that a value has been deleted
        size--;

        if (root instanceof NodeInternal<K, V> internalRoot && root.getKeys().isEmpty()) {
            // shrink tree
            root = internalRoot.getChildren().get(0);
            height--;
        }

        return deleteResult.value;
    }

    protected abstract class AbstractNode implements Node<K, V> {

        protected Node<K, V> parent;
        protected List<K> keys;

        protected AbstractNode(Node<K, V> parent) {
            this.keys = new ArrayList<>(order);
            this.parent = parent;
        }

        @Override
        public List<K> getKeys() {
            return keys;
        }

        @Override
        public Node<K, V> getParent() {
            return parent;
        }

        @Override
        public void setParent(Node<K, V> parent) {
            this.parent = parent;
        }
    }

    /**
     * Internal non-leaf node in the tree that contains only keys and child pointers
     */
    protected class InternalNode extends AbstractNode implements NodeInternal<K, V> {

        public List<Node<K, V>> children;

        public InternalNode(Node<K, V> parent) {
            super(parent);
            children = new ArrayList<>(order);
        }

        @Override
        public List<Node<K, V>> getChildren() {
            return children;
        }

        @Override
        public V search(K key) {
            int index = getChildIndex(key);
            return children.get(index).search(key);
        }

        @Override
        public NodeLeaf<K, V> findLeaf(K key) {
            int index = getChildIndex(key);
            Node<K, V> child = children.get(index);
            if (child instanceof NodeLeaf<K, V> leafChild) return leafChild;
            else return child.findLeaf(key);
        }

        @Override
        public int getKeyIndex(K key) {
            return Collections.binarySearch(keys, key, comparator);
        }

        /**
         * Get the index of a child, or where a child should be put if the key is not present
         */
        private int getChildIndex(K key) {
            int insertionPoint = getKeyIndex(key);
            if (insertionPoint >= 0) return insertionPoint + 1; // if exact match go to the right child
            else return -insertionPoint - 1; // if not found go to the appropriate child
        }

        @Override
        public SplitResult<K, V> insert(K key, V value) {
            int childIndex = getChildIndex(key);
            SplitResult<K, V> splitResult = children.get(childIndex).insert(key, value);
            splitResult.insertIndex = childIndex;
            // no split occurred in child, key was inserted
            if (splitResult.state != SplitState.SPLIT) {
                if (splitResult.state == SplitState.SPLIT_PREV_NODE) splitResult.state =
                    SplitState.EMPTY;
                return splitResult;
            }

            int insertionPoint = getKeyIndex(key);
            if (insertionPoint < 0) insertionPoint = -insertionPoint - 1;

            keys.add(insertionPoint, splitResult.key);
            // don't need to add left, because its already in this node
            children.add(insertionPoint + 1, splitResult.right);

            // check if node is full and needs to be split
            // return empty split result indicating that a new leaf node has been created
            if (keys.size() < order) {
                // return an empty split result indicating that a leaf node was split
                //                splitResult = new SplitResult<K, V>(insertionPoint);
                splitResult.insertIndex = insertionPoint;
                splitResult.state = SplitState.SPLIT_PREV_NODE;
                //                splitResult.right
                return splitResult;
            }

            int midPoint = keys.size() / 2;
            // move children and keys
            NodeInternal<K, V> newRight = BPlusTree.this.createInternalNode(parent);
            newRight.getKeys().addAll(keys.subList(midPoint + 1, keys.size()));
            newRight.getChildren().addAll(children.subList(midPoint + 1, children.size()));

            K splitKey = keys.get(midPoint);
            // update current keys and children
            keys = new ArrayList<>(keys.subList(0, midPoint));
            children = new ArrayList<>(children.subList(0, midPoint + 1));

            return new SplitResult<K, V>(
                insertionPoint,
                splitKey,
                this,
                newRight,
                SplitState.SPLIT
            );
        }

        @Override
        public DeleteResult<K, V> delete(K key, int parentIndex) {
            int childIndex = getChildIndex(key);
            DeleteResult<K, V> deleteResult = children.get(childIndex).delete(key, childIndex);
            // check if the key was deleted (if it was present in the tree
            // if an empty delete result is returned a child node was merged and the tree
            // is no longer deficient
            if (
                !deleteResult.underflow &&
                deleteResult.key == null &&
                deleteResult.state != DeleteState.MERGED
            ) {
                return deleteResult;
            }

            // check if we need to replace a key in this node
            if (deleteResult.key != null && childIndex > 0 && childIndex - 1 < keys.size()) {
                // key is also present in the internal node
                // we know this because the deleted key was the first key in the child node
                keys.set(childIndex - 1, deleteResult.key);
            }
            // remove replacement key
            deleteResult.key = null;

            if (!deleteResult.underflow) {
                // pass an empty delete result indicating that a leaf node was deleted
                // and set state as MERGED_LEAF if the node was merged
                if (deleteResult.state == DeleteState.MERGED) deleteResult.state =
                    DeleteState.MERGED_LEAF;
                return deleteResult;
            }
            // unset underflow
            deleteResult.underflow = false;

            Node<K, V> leftSibling = null;
            Node<K, V> rightSibling = null;

            if (childIndex > 0) {
                // try to borrow from left sibling
                leftSibling = children.get(childIndex - 1);
                // check if we can borrow a key from the left sibling
                if (leftSibling.getKeys().size() > minKeys) {
                    borrowFromLeftSibling(childIndex, leftSibling);

                    deleteResult.state = DeleteState.EMPTY;
                    return deleteResult;
                }
            } else if (childIndex < children.size() - 1) {
                rightSibling = children.get(childIndex + 1);
                if (rightSibling.getKeys().size() > minKeys) {
                    borrowFromRightSibling(childIndex, rightSibling);

                    deleteResult.state = DeleteState.EMPTY;
                    return deleteResult;
                }
            }

            if (leftSibling != null) mergeWithSibling(
                leftSibling,
                children.get(childIndex),
                childIndex - 1
            );
            else if (rightSibling != null) mergeWithSibling(
                children.get(childIndex),
                rightSibling,
                childIndex
            );

            boolean underflow = parent != null && keys.size() < minKeys;
            // if we remove the smallest key and this node is the first sibling of the parent
            // we need to get a replacement for the removed key
            K keyReplacement = null;
            if (
                childIndex == 0 &&
                parent != null &&
                ((NodeInternal<K, V>) parent).getChildren().indexOf(this) > 0
            ) {
                keyReplacement = children.get(0).firstLeafKey();
            }

            // handle underflow in parent node
            deleteResult.key = keyReplacement;
            deleteResult.underflow = underflow;
            deleteResult.state = DeleteState.MERGED;
            return deleteResult;
        }

        /**
         * Borrow a key from the {@code leftSibling} and put it on the child node
         *
         * @param childIndex  the index of the child that needs a key
         * @param leftSibling the child that will lend a key
         */
        protected void borrowFromLeftSibling(int childIndex, Node<K, V> leftSibling) {
            Node<K, V> child = children.get(childIndex);

            if (
                child instanceof NodeInternal<K, V> childInternal &&
                leftSibling instanceof NodeInternal<K, V> leftInternal
            ) {
                // update the child and parent keys
                childInternal.getKeys().add(0, keys.get(childIndex - 1));
                keys.set(
                    childIndex - 1,
                    leftInternal.getKeys().remove(leftInternal.getKeys().size() - 1)
                );

                Node<K, V> movedChild = leftInternal
                    .getChildren()
                    .remove(leftInternal.getChildren().size() - 1);
                movedChild.setParent(childInternal);
                childInternal.getChildren().add(0, movedChild);
            } else if (
                child instanceof NodeLeaf<K, V> childLeaf &&
                leftSibling instanceof NodeLeaf<K, V> leftLeaf
            ) {
                // borrow immediate left key: last key in the left sibling
                K borrowedKey = leftLeaf.getKeys().remove(leftLeaf.getKeys().size() - 1);
                V borrowedValue = leftLeaf.getValues().remove(leftLeaf.getValues().size() - 1);
                // add borrowed keys and value to the start of the child leaf
                childLeaf.getKeys().add(0, borrowedKey);
                childLeaf.getValues().add(0, borrowedValue);
                // replace key in the parent of the leafs (the current node)
                keys.set(childIndex - 1, borrowedKey);
            }
        }

        /**
         * Borrow a key from the {@code rightSibling} and put it on the child node
         *
         * @param childIndex   the index of the child that needs a key
         * @param rightSibling the child that will lend a key
         */
        protected void borrowFromRightSibling(int childIndex, Node<K, V> rightSibling) {
            Node<K, V> child = children.get(childIndex);

            if (
                child instanceof NodeInternal<K, V> childInternal &&
                rightSibling instanceof NodeInternal<K, V> rightInternal
            ) {
                childInternal.getKeys().add(keys.get(childIndex));
                keys.set(childIndex, rightInternal.getKeys().remove(0));

                Node<K, V> movedChild = (Node<K, V>) rightInternal.getChildren().remove(0);
                movedChild.setParent(childInternal);

                childInternal.getChildren().add(movedChild);
            } else if (
                child instanceof NodeLeaf<K, V> childLeaf &&
                rightSibling instanceof NodeLeaf<K, V> rightLeaf
            ) {
                // borrow immediate right key: first key in right sibling
                K borrowedKey = rightLeaf.getKeys().remove(0);
                V borrowedValue = rightLeaf.getValues().remove(0);
                // add borrowed key and value to the end of the child leaf
                childLeaf.getKeys().add(borrowedKey);
                childLeaf.getValues().add(borrowedValue);
                // replace key in the parent of the leafs (the current node)
                keys.set(childIndex, rightLeaf.getKeys().get(0));
            }
        }

        /**
         * Merge siblings by merging the right into the left node and removing the right node
         *
         * @param left     node that receives keys and values from the other
         * @param right    node that will be merged (deleted)
         * @param keyIndex index of the deleted key in the current node
         */
        protected void mergeWithSibling(Node<K, V> left, Node<K, V> right, int keyIndex) {
            if (
                left instanceof NodeInternal<K, V> leftInternal &&
                right instanceof NodeInternal<K, V> rightInternal
            ) {
                leftInternal.getKeys().add(keys.get(keyIndex));
                leftInternal.getKeys().addAll(rightInternal.getKeys());
                // update parent pointers
                for (Node<K, V> rightChild : rightInternal.getChildren()) {
                    rightChild.setParent(leftInternal);
                }
                leftInternal.getChildren().addAll(rightInternal.getChildren());
            } else if (
                left instanceof NodeLeaf<K, V> leftLeaf && right instanceof NodeLeaf<K, V> rightLeaf
            ) {
                leftLeaf.getKeys().addAll(rightLeaf.getKeys());
                leftLeaf.getValues().addAll(rightLeaf.getValues());
                // update leaf pointers
                leftLeaf.setNext(rightLeaf.getNext());
            }

            keys.remove(keyIndex);
            children.remove(right);
        }

        @Override
        public K firstLeafKey() {
            return children.getFirst().firstLeafKey();
        }

        @Override
        public K lastLeafKey() {
            return children.getLast().lastLeafKey();
        }
    }

    /**
     * A node representing the leafs of the B+ tree.
     * This class holds the values of the tree.
     */
    protected class LeafNode extends AbstractNode implements NodeLeaf<K, V> {

        protected List<V> values;
        protected NodeLeaf<K, V> next;
        protected NodeLeaf<K, V> prev;

        public LeafNode(Node<K, V> parent) {
            super(parent);
            values = new ArrayList<>(order);
        }

        @Override
        public V search(K key) {
            int index = getKeyIndex(key);
            if (index >= 0) return values.get(index);
            else return null;
        }

        @Override
        public NodeLeaf<K, V> findLeaf(K key) {
            int index = getKeyIndex(key);
            if (index >= 0) return this;
            else return null;
        }

        @Override
        public int getKeyIndex(K key) {
            return Collections.binarySearch(keys, key, comparator);
        }

        @Override
        public SplitResult<K, V> insert(K key, V value) {
            int insertPoint = getKeyIndex(key);
            if (insertPoint >= 0) {
                // key already exists just update the value
                SplitResult<K, V> splitResult = new SplitResult<K, V>(insertPoint);
                splitResult.state = SplitState.REPLACED;
                splitResult.replacedValue = values.get(insertPoint);

                values.set(insertPoint, value);
                return splitResult;
            }
            insertPoint = -insertPoint - 1;
            keys.add(insertPoint, key);
            values.add(insertPoint, value);
            // check if node is full and needs to be split
            if (keys.size() < order) {
                SplitResult<K, V> splitResult = new SplitResult<>(insertPoint);
                splitResult.state = SplitState.INSERTED;
                return splitResult;
            }
            int midPoint = keys.size() / 2;

            // move values and keys
            NodeLeaf<K, V> newRight = BPlusTree.this.createLeafNode(parent);
            newRight.getKeys().addAll(keys.subList(midPoint, keys.size()));
            newRight.getValues().addAll(values.subList(midPoint, values.size()));
            // update leaf pointers
            newRight.setNext(this.next);
            newRight.setPrev(this);
            this.setNext(newRight);
            // update current node
            keys = keys.subList(0, midPoint);
            values = values.subList(0, midPoint);
            return new SplitResult<K, V>(
                insertPoint,
                newRight.getKeys().get(0),
                this,
                newRight,
                SplitState.SPLIT
            );
        }

        @Override
        public DeleteResult<K, V> delete(K key, int parentIndex) {
            int keyIndex = getKeyIndex(key);
            // check if key exists in the tree
            if (keyIndex < 0) {
                return new DeleteResult<K, V>(-1, null, null, false, DeleteState.NOT_FOUND);
            }
            // remove key and value
            keys.remove(keyIndex);
            V deletedValue = values.remove(keyIndex);

            boolean underflow = parent != null && keys.size() < minKeys;

            K keyReplacement = null;
            if (keyIndex == 0 && parent != null && parentIndex > 0) {
                keyReplacement = keys.isEmpty() ? null : keys.get(0);
            }

            return new DeleteResult<K, V>(
                keyIndex,
                keyReplacement,
                deletedValue,
                underflow,
                DeleteState.EMPTY
            );
        }

        @Override
        public K firstLeafKey() {
            return keys.getFirst();
        }

        @Override
        public K lastLeafKey() {
            return keys.getLast();
        }

        @Override
        public List<V> getValues() {
            return values;
        }

        @Override
        public NodeLeaf<K, V> getNext() {
            return next;
        }

        public void setNext(NodeLeaf<K, V> next) {
            this.next = next;
        }

        @Override
        public NodeLeaf<K, V> getPrev() {
            return prev;
        }

        public void setPrev(NodeLeaf<K, V> prev) {
            this.prev = prev;
        }
    }

    protected NodeInternal<K, V> createInternalNode(Node<K, V> parent) {
        return new InternalNode(parent);
    }

    protected NodeLeaf<K, V> createLeafNode(Node<K, V> parent) {
        return new LeafNode(parent);
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
    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        if (root == null) return false;
        Objects.requireNonNull(key);

        return root.search((K) key) != null;
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
        // TODO: make efficient bulk insert
        map.forEach(this::put);
    }

    @Override
    public void clear() {
        size = 0;
        nLeafs = 0;
        firstLeaf = null;
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
            return BPlusTree.this.iterator();
        }

        @Override
        public int size() {
            return BPlusTree.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return BPlusTree.this.containsValue(o);
        }

        @Override
        public boolean remove(Object o) {
            return BPlusTree.this.remove(o) != null;
        }

        @Override
        public void clear() {
            BPlusTree.this.clear();
        }

        @Override
        public Spliterator<V> spliterator() {
            return BPlusTree.this.spliterator();
        }
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return BPlusTree.this.entryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) return false;

            V val = BPlusTree.this.get(entry.getKey());
            return val != null && val.equals(entry.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) return false;
            return BPlusTree.this.remove(entry.getKey(), entry.getValue());
        }

        @Override
        public int size() {
            return BPlusTree.this.size();
        }

        @Override
        public void clear() {
            BPlusTree.this.clear();
        }

        @Override
        public Spliterator<Entry<K, V>> spliterator() {
            return BPlusTree.this.entrySpliterator();
        }
    }

    private class KeySet extends AbstractSet<K> {

        @Override
        public Iterator<K> iterator() {
            return BPlusTree.this.keyIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) return false;

            V val = BPlusTree.this.get(entry.getKey());
            return val != null && val.equals(entry.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) return false;
            return BPlusTree.this.remove(entry.getKey(), entry.getValue());
        }

        @Override
        public int size() {
            return BPlusTree.this.size();
        }

        @Override
        public void clear() {
            BPlusTree.this.clear();
        }

        @Override
        public Spliterator<K> spliterator() {
            return BPlusTree.this.keySpliterator();
        }
    }

    // =============================================
    //      Iterator and Stream implementations
    // =============================================

    /**
     * Returns an iterator of a part of the tree
     *
     * @param from    start key
     * @param to      (inclusive) end key
     * @param factory function that creates an instance of {@link BaseIterator}
     * @param <T>     the type that should be used in the iterator
     * @return an iterator of a part of the tree
     */
    private <T> Iterator<T> basePartialIterator(
        K from,
        K to,
        Function<NodeLeaf<K, V>, BaseIterator<K, T>> factory
    ) {
        if (root == null) return Collections.emptyIterator();
        if (
            from != null && to != null && comparator.compare(from, to) > 0
        ) return Collections.emptyIterator();

        // find start leaf
        NodeLeaf<K, V> fromLeaf = from == null ? firstLeaf : root.findLeaf(from);

        int leafIndex = from == null ? 0 : Collections.binarySearch(fromLeaf.getKeys(), from);
        if (leafIndex < 0) leafIndex = -leafIndex - 1;

        if (leafIndex >= fromLeaf.getKeys().size()) {
            leafIndex = 0;
            fromLeaf = fromLeaf.getNext();
            if (fromLeaf == null) return Collections.emptyIterator();
        }

        BaseIterator<K, T> it = factory.apply(fromLeaf);
        it.setLeafIndex(leafIndex);
        if (to != null) it.setStopKey(to);
        return it;
    }

    @Override
    public Iterator<V> iterator() {
        return new ValueIterator<>(firstLeaf);
    }

    /**
     * Returns an iterator that starts at {@code from} and stops when the next {@code key > to}
     *
     * @param from the start key (can be {@code null} to begin at the start)
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
        return new KeyIterator<>(firstLeaf);
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
        return new EntryIterator<>(firstLeaf);
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
        return new ValueSpliterator<>(firstLeaf, 0, nLeafs, size);
    }

    /**
     * @return a spliterator of the keys
     */
    public Spliterator<K> keySpliterator() {
        return new KeySpliterator<>(firstLeaf, 0, nLeafs, size);
    }

    /**
     * @return a spliterator of the keys
     */
    public Spliterator<Map.Entry<K, V>> entrySpliterator() {
        return new EntrySpliterator<>(firstLeaf, 0, nLeafs, size);
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
        Spliterator<Map.Entry<K, V>> split = new EntrySpliterator<>(firstLeaf, 0, nLeafs, size);
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

    /**
     * Base {@link Iterator} class
     *
     * @param <T> the value that should be returned in the iterator
     */
    private abstract static class BaseIterator<K extends Comparable<K>, T> implements Iterator<T> {

        protected NodeLeaf<K, ?> currentLeaf;
        protected int leafIndex = 0;
        protected K stopKey = null;

        public BaseIterator(NodeLeaf<K, ?> startLeaf) {
            this.currentLeaf = startLeaf;
        }

        public void setStopKey(K key) {
            this.stopKey = key;
        }

        public void setLeafIndex(int index) {
            this.leafIndex = index;
        }

        @Override
        public boolean hasNext() {
            if (currentLeaf == null) return false;

            boolean isStopPoint =
                stopKey != null && currentLeaf.getKeys().get(leafIndex).compareTo(stopKey) > 0;
            return !isStopPoint;
        }

        @Override
        public T next() {
            T result = get();
            leafIndex++;

            if (leafIndex >= currentLeaf.getKeys().size()) {
                currentLeaf = currentLeaf.getNext();
                leafIndex = 0;
            }
            return result;
        }

        protected abstract T get();
    }

    /**
     * Iterator of values in the tree
     */
    private static class ValueIterator<K extends Comparable<K>, V> extends BaseIterator<K, V> {

        public ValueIterator(NodeLeaf<K, V> startLeaf) {
            super(startLeaf);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected V get() {
            return (V) currentLeaf.getValues().get(leafIndex);
        }
    }

    /**
     * Iterator of the keys in the tree
     */
    private static class KeyIterator<K extends Comparable<K>, V> extends BaseIterator<K, K> {

        public KeyIterator(NodeLeaf<K, V> startLeaf) {
            super(startLeaf);
        }

        @Override
        protected K get() {
            return currentLeaf.getKeys().get(leafIndex);
        }
    }

    /**
     * iterator of entries in the tree
     */
    private static class EntryIterator<K extends Comparable<K>, V>
        extends BaseIterator<K, Entry<K, V>> {

        public EntryIterator(NodeLeaf<K, V> startLeaf) {
            super(startLeaf);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Map.Entry<K, V> get() {
            K key = currentLeaf.getKeys().get(leafIndex);
            V value = (V) currentLeaf.getValues().get(leafIndex);
            return Map.entry(key, value);
        }
    }

    /**
     * Base {@link Spliterator} class
     */
    private abstract static class BaseSpliterator<K extends Comparable<K>, T>
        implements Spliterator<T> {

        protected NodeLeaf<K, ?> currentLeaf;
        protected int leafIndex;
        protected int remainingLeafs;
        protected int remainingValues;

        public interface SpliteratorFactory<K extends Comparable<K>, T> {
            BaseSpliterator<K, T> create(
                NodeLeaf<K, ?> startLeaf,
                int leafIndex,
                int size,
                int totalValues
            );
        }

        private final SpliteratorFactory<K, T> factory;

        public BaseSpliterator(
            NodeLeaf<K, ?> startLeaf,
            int leafIndex,
            int size,
            int totalValues,
            SpliteratorFactory<K, T> factory
        ) {
            this.currentLeaf = startLeaf;
            this.leafIndex = leafIndex;
            this.remainingLeafs = size;
            this.remainingValues = totalValues;
            this.factory = factory;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (remainingValues <= 0) return false;

            action.accept(get());
            leafIndex++;

            // move to next position
            if (leafIndex >= currentLeaf.getValues().size()) {
                currentLeaf = currentLeaf.getNext();
                leafIndex = 0;
                remainingLeafs--;
            }
            remainingValues--;

            return true;
        }

        @Override
        public Spliterator<T> trySplit() {
            // don't split if there aren't that many values
            if (remainingValues < 1000) return null;

            int splitSize = remainingLeafs / 2;

            int oldIndex = leafIndex;
            NodeLeaf<K, ?> oldLeaf = currentLeaf;

            int toSkip = splitSize;
            int totalValues = 0;
            while (toSkip-- > 0) {
                currentLeaf = currentLeaf.getNext();
                totalValues += currentLeaf.getValues().size();
            }

            // create new spliterator for the first half
            BaseSpliterator<K, T> split = factory.create(oldLeaf, oldIndex, splitSize, totalValues);
            // update the current spliterator which will iterate over the second half
            remainingLeafs -= splitSize;
            remainingValues -= totalValues;
            return split;
        }

        @Override
        public long estimateSize() {
            return remainingValues;
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

    /**
     * Spliterator of values in the tree
     */
    private static class ValueSpliterator<K extends Comparable<K>, V>
        extends BaseSpliterator<K, V> {

        @SuppressWarnings("unchecked")
        public ValueSpliterator(
            NodeLeaf<K, V> startLeaf,
            int leafIndex,
            int size,
            int totalValues
        ) {
            super(startLeaf, leafIndex, size, totalValues, (leaf, index, sz, total) ->
                new ValueSpliterator<>((NodeLeaf<K, V>) leaf, index, sz, total)
            );
        }

        @Override
        @SuppressWarnings("unchecked")
        protected V get() {
            return (V) currentLeaf.getValues().get(leafIndex);
        }
    }

    /**
     * Spliterator of keys in the tree
     */
    private static class KeySpliterator<K extends Comparable<K>, V> extends BaseSpliterator<K, K> {

        @SuppressWarnings("unchecked")
        public KeySpliterator(NodeLeaf<K, V> startLeaf, int leafIndex, int size, int totalValues) {
            super(startLeaf, leafIndex, size, totalValues, (leaf, index, sz, total) ->
                new KeySpliterator<>((NodeLeaf<K, V>) leaf, index, sz, total)
            );
        }

        @Override
        protected K get() {
            return currentLeaf.getKeys().get(leafIndex);
        }
    }

    /**
     * Spliterator of entries in the tree
     */
    private static class EntrySpliterator<K extends Comparable<K>, V>
        extends BaseSpliterator<K, Entry<K, V>> {

        @SuppressWarnings("unchecked")
        public EntrySpliterator(
            NodeLeaf<K, V> startLeaf,
            int leafIndex,
            int size,
            int totalValues
        ) {
            super(startLeaf, leafIndex, size, totalValues, (leaf, index, sz, total) ->
                new EntrySpliterator<>((NodeLeaf<K, V>) leaf, index, sz, total)
            );
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Map.Entry<K, V> get() {
            K key = currentLeaf.getKeys().get(leafIndex);
            V value = (V) currentLeaf.getValues().get(leafIndex);
            return Map.entry(key, value);
        }
    }
}
