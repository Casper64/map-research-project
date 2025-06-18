import static org.junit.jupiter.api.Assertions.*;

import com.casperswebsites.trees.bplustree.BPlusTree;
import java.util.Comparator;
import java.util.Iterator;
import org.junit.Test;

public class BPlusTreeTest {

    @Test
    public void insert() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(3);

        for (int i = 1; i <= 5; i++) {
            tree.put(i, Integer.toString(i));
        }

        assertEquals(5, tree.size());
        assertEquals(4, tree.getNLeafs());
        assertEquals(3, tree.getHeight());

        for (int i = 1; i <= 5; i++) {
            assertEquals(Integer.toString(i), tree.get(i));
        }
    }

    @Test
    public void replaceKey() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(3);

        tree.put(1, "1");
        tree.put(2, "2");
        tree.put(3, "3");
        tree.put(4, "4");
        tree.put(5, "5");
        assertEquals("12345", collectToString(tree));

        tree.put(3, "three");
        assertEquals("12three45", collectToString(tree));
        assertEquals(3, tree.getHeight());
    }

    @Test
    public void search() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(3);
        for (int i = 1; i <= 5; i++) {
            tree.put(i, Integer.toString(i));
        }

        assertNotNull(tree.get(5));
        assertNull(tree.get(6));
        assertNull(tree.get(-1));
    }

    @Test
    public void iterator() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(3);
        for (int i = 1; i <= 5; i++) {
            tree.put(i, Integer.toString(i));
        }

        int counter = 1;
        for (String value : tree) {
            assertEquals(Integer.toString(counter), value);
            counter++;
        }
    }

    @Test
    public void partialIterator() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(3);
        for (int i = 1; i <= 5; i++) {
            tree.put(i, Integer.toString(i));
        }

        int counter = 2;
        Iterator<String> it = tree.iterator(2, 4);
        while (it.hasNext()) {
            String value = it.next();
            assertEquals(Integer.toString(counter), value);
            counter++;
        }
        assertFalse(it.hasNext());
        assertEquals(5, counter);
    }

    @Test
    public void stream() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(3);

        tree.put(1, "a");
        tree.put(5, "e");
        tree.put(2, "b");
        tree.put(4, "d");
        tree.put(3, "c");

        String actual = tree.stream().reduce("", String::concat);
        assertEquals("abcde", actual);
    }

    /**
     * Class that encapsulates an integer but inverses comparison.
     * E.g. {@code 5 > 2 = true} and {@code 1 < 3 = false}
     */
    private static class InvertedInteger implements Comparable<InvertedInteger> {

        public Integer value;

        public InvertedInteger(Integer value) {
            this.value = value;
        }

        @Override
        public int compareTo(InvertedInteger o) {
            // invert the result of compare to
            return -value.compareTo(o.value);
        }
    }

    @Test
    public void comparableKey() {
        BPlusTree<InvertedInteger, String> tree = new BPlusTree<>(3);

        tree.put(new InvertedInteger(1), "a");
        tree.put(new InvertedInteger(5), "e");
        tree.put(new InvertedInteger(2), "b");
        tree.put(new InvertedInteger(4), "d");
        tree.put(new InvertedInteger(3), "c");

        String actual = tree.stream().reduce("", String::concat);
        assertEquals("edcba", actual);
    }

    @Test
    public void customComparator() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(
            3,
            new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    // invert the result of compare to
                    return -Integer.compare(o1, o2);
                }
            }
        );

        tree.put(1, "a");
        tree.put(5, "e");
        tree.put(2, "b");
        tree.put(4, "d");
        tree.put(3, "c");

        String actual = tree.stream().reduce("", String::concat);
        assertEquals("edcba", actual);
    }

    private BPlusTree<Integer, String> getDeleteTree() {
        // example tree: https://www.programiz.com/dsa/deletion-from-a-b-plus-tree
        // all tests follow the deletions in this tree

        BPlusTree<Integer, String> tree = new BPlusTree<>(3);
        tree.put(5, "a");
        tree.put(15, "b");
        tree.put(25, "d");
        tree.put(35, "f");
        tree.put(45, "h");
        tree.put(20, "c");
        tree.put(55, "i");
        tree.put(30, "e");
        tree.put(40, "g");

        return tree;
    }

    private String collectToString(BPlusTree<?, ?> tree) {
        return tree.stream().map(Object::toString).reduce("", String::concat);
    }

    @Test
    public void deleteCase1() {
        BPlusTree<Integer, String> tree = getDeleteTree();
        assertEquals(5, tree.getNLeafs());

        // case 1.1 node still has minimum number of keys
        String v = tree.remove(40);
        assertEquals("g", v);

        assertNull(tree.get(40));
        assertEquals(8, tree.size());
        assertEquals(5, tree.getNLeafs());
        assertEquals(3, tree.getHeight());
        assertEquals("abcdefhi", collectToString(tree));

        // case 1.2 node has less than the minimum number of keys

        v = tree.remove(5);
        assertEquals("a", v);

        assertNull(tree.get(5));
        assertEquals(7, tree.size());
        assertEquals(5, tree.getNLeafs());
        assertEquals(3, tree.getHeight());
        assertEquals("bcdefhi", collectToString(tree));

        // assert all tree values are still ordered correctly
        int[] searchableNodes = new int[] { 15, 20, 25, 30, 35, 45, 55 };
        for (int searchableNode : searchableNodes) {
            assertNotNull(tree.get(searchableNode));
        }
    }

    @Test
    public void deleteCase2() {
        BPlusTree<Integer, String> tree = getDeleteTree();
        assertEquals(5, tree.getNLeafs());
        tree.remove(40);
        tree.remove(5);

        String v = tree.remove(45);
        assertEquals("h", v);

        assertNull(tree.get(45));
        assertEquals(6, tree.size());
        assertEquals(5, tree.getNLeafs());
        assertEquals(3, tree.getHeight());
        assertEquals("bcdefi", collectToString(tree));

        v = tree.remove(35);
        assertEquals("f", v);

        assertNull(tree.get(35));
        assertEquals(5, tree.size());
        assertEquals(5, tree.getNLeafs());
        assertEquals(3, tree.getHeight());
        assertEquals("bcdei", collectToString(tree));

        v = tree.remove(25);
        assertEquals("d", v);

        assertNull(tree.get(25));
        assertEquals(4, tree.size());
        assertEquals(4, tree.getNLeafs());
        assertEquals(3, tree.getHeight());
        assertEquals("bcei", collectToString(tree));

        // assert all tree values are still ordered correctly
        int[] searchableNodes = new int[] { 15, 20, 30, 55 };
        for (int searchableNode : searchableNodes) {
            assertNotNull(tree.get(searchableNode));
        }
    }

    @Test
    public void deleteCase3() {
        BPlusTree<Integer, String> tree = getDeleteTree();
        tree.remove(40);
        tree.remove(5);
        tree.remove(45);
        tree.remove(35);
        tree.remove(25);

        String v = tree.remove(55);
        assertEquals("i", v);

        assertNull(tree.get(55));
        assertEquals(3, tree.size());
        assertEquals(3, tree.getNLeafs());
        // test that the tree has shrunk
        assertEquals(2, tree.getHeight());
        assertEquals("bce", collectToString(tree));

        // assert all tree values are still ordered correctly
        int[] searchableNodes = new int[] { 15, 20, 30 };
        for (int searchableNode : searchableNodes) {
            assertNotNull(tree.get(searchableNode));
        }
    }

    @Test
    public void deleteAll() {
        BPlusTree<Integer, String> tree = getDeleteTree();
        tree.remove(40);
        tree.remove(5);
        tree.remove(45);
        tree.remove(35);
        tree.remove(25);
        tree.remove(55);

        String v = tree.remove(20);
        assertEquals("c", v);

        assertNull(tree.get(20));
        assertEquals(2, tree.size());
        // tree has shrunk
        assertEquals(2, tree.getNLeafs());
        assertEquals("be", collectToString(tree));
        // assert all tree values are still ordered correctly
        int[] searchableNodes = new int[] { 15, 30 };
        for (int searchableNode : searchableNodes) {
            assertNotNull(tree.get(searchableNode));
        }

        v = tree.remove(30);
        assertEquals("e", v);

        assertNull(tree.get(30));
        assertEquals(1, tree.size());
        assertEquals(1, tree.getNLeafs());
        assertEquals(1, tree.getHeight());
        assertEquals("b", collectToString(tree));
        // assert all tree values are still ordered correctly
        searchableNodes = new int[] { 15 };
        for (int searchableNode : searchableNodes) {
            assertNotNull(tree.get(searchableNode));
        }

        v = tree.remove(15);
        assertEquals("b", v);

        assertNull(tree.get(15));
        assertEquals(0, tree.size());
        assertEquals(0, tree.getNLeafs());
        assertEquals("", collectToString(tree));
    }

    @Test
    public void borrowFromInternalLeftSibling() {
        BPlusTree<Integer, String> tree = getDeleteTree();

        tree.remove(35);
        tree.remove(40);
        tree.remove(45);
        tree.remove(55);
        tree.put(22, "_");
        tree.remove(30);

        assertEquals(5, tree.size());
        assertEquals(5, tree.getNLeafs());
        assertEquals(3, tree.getHeight());
        assertEquals("abc_d", collectToString(tree));
    }

    @Test
    public void borrowFromInternalRightSibling() {
        BPlusTree<Integer, String> tree = getDeleteTree();

        tree.remove(5);
        tree.remove(20);

        assertNull(tree.get(5));
        assertNull(tree.get(20));
        assertEquals(7, tree.size());
        assertEquals(5, tree.getNLeafs());
        assertEquals(3, tree.getHeight());
        assertEquals("bdefghi", collectToString(tree));
    }
}
