# Map Comparison Research Project

Research project comparing the performance of different map (tree) data structures in Java.

## Motivation

I was interested in Tree data structures and noticed that the Java Collection Framework does not
provide a B+ tree implementation out of the box. I wanted to test my skills by implementing a
functional B+ tree and see how it compares performance-wise to other tree data structures.

I chose Red-Black trees and B+ trees, because they are both sorted tree data structures with
the same time complexity for search, insert, and delete operations (O(log n)).
So when comparing them, will one be faster than the other?

## Implemented Data Structures

- B+ Tree
- RedBlackTree (Java's `TreeMap` uses this implementation)

The classes are made for educational purposes and not necessarily with performance in mind.
Tests are included to verify the correctness of the implementations.

To make my implementations more usable I also added support for iterators and streams to both trees.

## Hypothesis

1. B+ trees store records in leaf nodes which are linked together in a linked list. I expect that
   this will make range queries (iteration over a range of values) faster than in Red-Black trees,
   where nodes are not linked together and an iterator needs to traverse the tree.

2. I also expect that B+ trees will be slower at insertions and deletions, because the splitting and
   merging of nodes seems to me like a more expensive operation than the rotations in Red-Black trees.
   Although it will be interesting to see how the order of a B+ tree affects this (maximum number of records
   per leaf node).

## Method

I ran performance tests for the following operations:

- Random Lookups
- Sequential Insertions
- Random Insertions
- Sequential Deletions
- Random Deletions
- Full Iteration (iterating over all values)
- Partial Iteration (iterating over a range of values)

Any random elements are generated using Java's `Random` class with a fixed seed to ensure reproducibility.

> **Note:**
> I did this project for a school assignment and we weren't allowed to use benchmarking libraries
> like JMH(?? why). Therefore I created my own. that runs each test multiple times and include warmup
> runs to get more reliable results.

## Results

You can easily view the results in the [notebook](visual.ipynb) or check out the images in the `results` folder.

As a baseline I also included Java's `TreeMap` and `HashMap` implementations in the performance tests.

1. The `HashMap` is the winner for all performance tests, except for partial iteration.
2. The B+ tree consistently outperforms the red-black tree implementations on insertions and iteration
   and lookups, dependent on the order of the B+ tree.
3. As expected red-black trees are faster at deletions.
4. The order of a B+ tree has significant impact on its performance.

> **Note:**
> These results where ran using Java 25.0.2
> Props to the Java maintainers for improving the perforamce of `HashMap` between Java 21 and 25.
> When I first made this project in Java 21 the B+ tree outperformed hash maps on iteration by a large margin.

### Some other observations

To my surprise my implementation of a red-black tree was on par with `TreeMap`, except for the iteration.
I think that is due to the fact that I found a really [good article on red-black trees](https://www.happycoders.eu/algorithms/red-black-tree-java/).
I then changed the implementations from a recursive to an iterative approach and
found a way to reduce the number of if-statements while fixing the red-black tree after an insertion and deletion.
Maybe that is why my implementation outshines the `TreeMap` in random insertions for smaller amounts of data.

### Future Ideas

The performance of a B+ tree is highly dependent on its order (maximum number of records per leaf node).
I would like to run more tests with different orders to see how it affects the performance.
And / or optimise the order for the data size of the keys and values to fit cache lines.

This aspect isn't covered in this project.

## Usage

Run the performance tests with

```bash
mvn compile && java -Xms4g -Xmx4g -cp "target/classes com.casperswebsites.trees.PerformanceTestStructured"
```

to get the output data in `csv` format.

This csv is later used in `visual.ipynb` to create the graphs that I used.

### Notebook

The results are visualized in a [Jupyter notebook](visual.ipynb).

You can run it yourself by installing Jupyter and the packages in `requirements.txt`.

### Visualisation

By default the visualisation is shown on a logarithmic scale to better illustrate the differences.
You can adjust the scale in the notebook.

The notebook also outputs the results as `png` files in the `results` folder.
