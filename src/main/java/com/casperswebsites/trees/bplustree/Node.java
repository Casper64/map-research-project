/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 Casper Küthe <casperck64@gmail.com>
 * SPDX-License-Identifier: MIT
 */

package com.casperswebsites.trees.bplustree;

import java.util.List;

/**
 * Abstract base class for the nodes in a B+ tree
 */
public interface Node<K extends Comparable<K>, V> {
    boolean isLeaf();

    Node<K, V> getParent();
    void setParent(Node<K, V> parent);
    List<K> getKeys();

    /**
     * Insert a key value pair and returns the result in {@link SplitResult<K, V>}
     *
     * @param key
     * @param value
     * @return the result of the split
     */
    SplitResult<K, V> insert(K key, V value);

    /**
     * @param key
     * @return the value that belongs to {@code key}
     * @null when the key is not present in the tree
     */
    V search(K key);

    NodeLeaf<K, V> findLeaf(K key);

    /**
     * Delete a key value pair and returns the result in {@link SplitResult<K, V>}
     *
     * @param key         the key to be deleted
     * @param parentIndex the index of this node in the parent.
     *                    Must be {@code -1} if the node has no parent
     * @return the result of the deletion
     */
    DeleteResult<K, V> delete(K key, int parentIndex);

    /**
     * @return the first leaf key (smallest value) in the subtree of this node and its children
     */
    K firstLeafKey();

    /**
     * @return the last leaf key (biggest value) in the subtree of this node and its children
     */
    K lastLeafKey();

    /**
     * Returns the index of the given key in the keys array. Behaves like {@code binarySearch}
     * in the {@link java.util.Collections} class.
     * @param key the key to search for
     * @return
     * the index of the search key, if it is contained in the list; otherwise,
     * (-(insertion point) - 1). The insertion point is defined as the point at
     * which the key would be inserted into the list: the index of the first element
     * greater than the key, or list.size() if all elements in the list are less than
     * the specified key. Note that this guarantees that the return value will be >= 0
     * if and only if the key is found.
     */
    int getKeyIndex(K key);
}
