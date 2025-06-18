import static org.junit.jupiter.api.Assertions.*;

import com.casperswebsites.trees.redblack.RedBlackTree;
import java.util.Comparator;
import java.util.Iterator;
import org.junit.Test;

public class RedBlackTreeTest {

    @Test
    public void insert() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();

        for (int i = 1; i <= 5; i++) {
            tree.put(i, Integer.toString(i));
        }

        assertEquals(5, tree.size());

        for (int i = 1; i <= 5; i++) {
            assertEquals(Integer.toString(i), tree.get(i));
        }
    }

    @Test
    public void replaceKey() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();

        tree.put(1, "1");
        tree.put(2, "2");
        tree.put(3, "3");
        tree.put(4, "4");
        tree.put(5, "5");
        assertEquals("12345", collectToString(tree));

        tree.put(3, "three");
        assertEquals("12three45", collectToString(tree));
    }

    @Test
    public void search() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        for (int i = 1; i <= 5; i++) {
            tree.put(i, Integer.toString(i));
        }

        assertNotNull(tree.get(5));
        assertNull(tree.get(6));
        assertNull(tree.get(-1));
    }

    @Test
    public void iterator() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
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
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
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
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();

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
        RedBlackTree<InvertedInteger, String> tree = new RedBlackTree<>();

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
        RedBlackTree<Integer, String> tree = new RedBlackTree<>(
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

    private RedBlackTree<Integer, String> getDeleteTree() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
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

    private String collectToString(RedBlackTree<?, ?> tree) {
        return tree.stream().map(Object::toString).reduce("", String::concat);
    }

    @Test
    public void deleteTest() {
        var tree = getDeleteTree();

        tree.remove(40);
        assertEquals("abcdefhi", collectToString(tree));

        tree.remove(5);
        assertEquals("bcdefhi", collectToString(tree));
    }

    @Test
    public void deleteNonExistentKey() {
        var tree = getDeleteTree();
        int originalSize = tree.size();

        assertNull(tree.remove(100)); // Key doesn't exist
        assertEquals(originalSize, tree.size());
        assertEquals("abcdefghi", collectToString(tree));
    }

    @Test
    public void deleteFromEmptyTree() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        assertNull(tree.remove(5));
        assertEquals(0, tree.size());
    }

    @Test
    public void deleteSingleNodeTree() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        tree.put(10, "single");

        assertEquals("single", tree.remove(10));
        assertEquals(0, tree.size());
        assertNull(tree.get(10));
    }

    @Test
    public void deleteRoot() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        tree.put(10, "root");
        tree.put(5, "left");
        tree.put(15, "right");

        assertEquals("root", tree.remove(10));
        assertEquals(2, tree.size());
        assertEquals("leftright", collectToString(tree));
    }

    @Test
    public void deleteLeafNodes() {
        var tree = getDeleteTree();

        // Delete leaf nodes
        assertEquals("c", tree.remove(20)); // leaf
        assertEquals("abdefghi", collectToString(tree));

        assertEquals("e", tree.remove(30)); // leaf
        assertEquals("abdfghi", collectToString(tree));

        assertEquals("i", tree.remove(55)); // leaf
        assertEquals("abdfgh", collectToString(tree));
    }

    @Test
    public void deleteNodeWithOneChild() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        tree.put(10, "root");
        tree.put(5, "left");
        tree.put(15, "right");
        tree.put(3, "leftleft");
        tree.put(7, "leftright");
        tree.put(17, "rightright");

        // Delete node with only right child
        tree.remove(15);
        assertEquals("leftleftleftleftrightrootrightright", collectToString(tree));

        // Delete node with two children (left subtree)
        tree.remove(5);
        assertEquals("leftleftleftrightrootrightright", collectToString(tree));
    }

    @Test
    public void deleteNodeWithTwoChildren() {
        var tree = getDeleteTree();

        // Delete node with two children - should replace with inorder successor
        assertEquals("b", tree.remove(15)); // Has children 20 and 25
        assertEquals("acdefghi", collectToString(tree));

        assertEquals("a", tree.remove(5)); // Root node with two children
        assertEquals("cdefghi", collectToString(tree));
    }

    @Test
    public void deleteAllAsc() {
        var tree = getDeleteTree();
        String[] expected = {
            "abcdefghi",
            "bcdefghi",
            "cdefghi",
            "defghi",
            "efghi",
            "fghi",
            "ghi",
            "hi",
            "i",
            "",
        };

        assertEquals(expected[0], collectToString(tree));

        int[] toRemove = { 5, 15, 20, 25, 30, 35, 40, 45, 55 };
        for (int i = 0; i < toRemove.length; i++) {
            tree.remove(toRemove[i]);
            assertEquals(expected[i + 1], collectToString(tree));
        }
    }

    @Test
    public void deleteAllDesc() {
        var tree = getDeleteTree();

        int[] toRemove = { 55, 45, 40, 35, 30, 25, 20, 15, 5 };
        String[] expected = {
            "abcdefgh",
            "abcdefg",
            "abcdef",
            "abcde",
            "abcd",
            "abc",
            "ab",
            "a",
            "",
        };

        for (int i = 0; i < toRemove.length; i++) {
            tree.remove(toRemove[i]);
            assertEquals(expected[i], collectToString(tree));
        }
    }

    @Test
    public void deleteRandom() {
        var tree = getDeleteTree();

        tree.remove(25);
        assertEquals("abcefghi", collectToString(tree));

        tree.remove(35);
        assertEquals("abceghi", collectToString(tree));

        tree.remove(15);
        assertEquals("aceghi", collectToString(tree));

        tree.remove(55);
        assertEquals("acegh", collectToString(tree));

        tree.remove(5);
        assertEquals("cegh", collectToString(tree));
    }

    @Test
    public void deleteAndReinsert() {
        var tree = getDeleteTree();

        // Delete some nodes
        tree.remove(20);
        tree.remove(40);
        assertEquals("abdefhi", collectToString(tree));

        // Reinsert them with different values
        tree.put(20, "NEW20");
        tree.put(40, "NEW40");
        assertEquals("abNEW20defNEW40hi", collectToString(tree));

        // Delete and reinsert with same values
        tree.remove(20);
        tree.remove(40);
        tree.put(20, "c");
        tree.put(40, "g");
        assertEquals("abcdefghi", collectToString(tree));
    }

    @Test
    public void deleteMinAndMax() {
        var tree = getDeleteTree();

        // Delete minimum (leftmost)
        assertEquals("a", tree.remove(5));
        assertEquals("bcdefghi", collectToString(tree));

        // Delete maximum (rightmost)
        assertEquals("i", tree.remove(55));
        assertEquals("bcdefgh", collectToString(tree));

        // Delete new minimum and maximum
        assertEquals("b", tree.remove(15));
        assertEquals("cdefgh", collectToString(tree));

        assertEquals("h", tree.remove(45));
        assertEquals("cdefg", collectToString(tree));
    }

    @Test
    public void deleteReturnValues() {
        var tree = getDeleteTree();

        // Test that correct values are returned
        assertEquals("a", tree.remove(5));
        assertEquals("b", tree.remove(15));
        assertEquals("c", tree.remove(20));
        assertEquals("d", tree.remove(25));
        assertEquals("e", tree.remove(30));
        assertEquals("f", tree.remove(35));
        assertEquals("g", tree.remove(40));
        assertEquals("h", tree.remove(45));
        assertEquals("i", tree.remove(55));

        assertEquals(0, tree.size());
        assertEquals("", collectToString(tree));
    }
}
