/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 Casper Küthe <casperck64@gmail.com>
 * SPDX-License-Identifier: MIT
 */

package com.casperswebsites.trees.bplustree;

/**
 * Holds the result of a node split. <br>
 * {@code isReplaced = true} if a value was replaced <br>
 * {@code isEmpty = true} if a leaf node was split in a child <br>
 * {@code null} if no split occurred and the value was inserted <br>
 * else the current node was split and a new node has to be inserted in the parent
 */
public class SplitResult<K extends Comparable<K>, V> {

    public int insertIndex;
    public K key;
    public Node<K, V> left;
    public Node<K, V> right;
    //    public boolean empty = false;
    //    public boolean replaced = false;
    public SplitState state;
    public V replacedValue;

    public SplitResult(
        int insertIndex,
        K key,
        Node<K, V> left,
        Node<K, V> right,
        SplitState state
    ) {
        this.insertIndex = insertIndex;
        this.key = key;
        this.left = left;
        this.right = right;
        this.state = state;
    }

    public SplitResult(int insertIndex) {
        this.state = SplitState.EMPTY;
        this.insertIndex = insertIndex;
    }
}
