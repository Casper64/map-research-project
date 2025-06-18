# Readme

Run performance tests with

```
mvn compile && java -Xms4g -Xmx4g \
  -cp target/classes com.casperswebsites.trees.PerformanceTest
```

Likewise you can pass `PerformanceTestStructured` to get the output data in `csv` format.

This csv is later used in `visual.ipynb` to create the graphs that I used.
