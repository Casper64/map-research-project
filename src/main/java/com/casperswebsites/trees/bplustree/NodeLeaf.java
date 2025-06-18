/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 Casper Küthe <casperck64@gmail.com>
 * SPDX-License-Identifier: MIT
 */

package com.casperswebsites.trees.bplustree;

import java.util.List;

public interface NodeLeaf<K extends Comparable<K>, V> extends Node<K, V> {
    @Override
    default boolean isLeaf() {
        return true;
    }

    List<V> getValues();

    NodeLeaf<K, V> getNext();
    void setNext(NodeLeaf<K, V> next);
    NodeLeaf<K, V> getPrev();
    void setPrev(NodeLeaf<K, V> previous);
}
