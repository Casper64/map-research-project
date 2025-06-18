/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 Casper Küthe <casperck64@gmail.com>
 * SPDX-License-Identifier: MIT
 */

package com.casperswebsites.trees.bplustree;

import java.util.List;

public interface NodeInternal<K extends Comparable<K>, V> extends Node<K, V> {
    @Override
    default boolean isLeaf() {
        return false;
    }

    List<Node<K, V>> getChildren();
}
