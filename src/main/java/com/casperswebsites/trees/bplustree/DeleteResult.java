/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 Casper Küthe <casperck64@gmail.com>
 * SPDX-License-Identifier: MIT
 */

package com.casperswebsites.trees.bplustree;

/**
 * Holds the result of a node deletion. <br>
 * {@code underflow = true} if the node has less then the minimum nr. of keys <br>
 */
public class DeleteResult<K extends Comparable<K>, V> {

    public int deleteIndex;
    public K key;
    public V value;
    public boolean underflow;
    public DeleteState state;

    public DeleteResult(int deleteIndex, K key, V value, boolean underflow, DeleteState state) {
        this.deleteIndex = deleteIndex;
        this.key = key;
        this.value = value;
        this.underflow = underflow;
        this.state = state;
    }
}
