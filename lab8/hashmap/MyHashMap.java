package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private int size;
    private double maxLoad;
    private Collection<Node>[] buckets;
    private  int length;
    // You should probably define some more!

    /** Constructors */
    public MyHashMap() {
        this(16, 0.75);
    }

    public MyHashMap(int initialSize) {
        this(initialSize, 0.75);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        this.size = initialSize;
        this.maxLoad = maxLoad;
        length = 0;
        buckets = createTable(size);
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new HashSet<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        return new Collection[tableSize];
    }

    @Override
    public void clear() {
        size = 16;
        length = 0;
        maxLoad = 0.75;
        buckets = createTable(size);
    }

    @Override
    public Iterator<K> iterator() {
        return new Iterator<K>() {
            private int pos = findNextEmptyBucket(0);
            private Collection<Node> curBucket = buckets[pos];
            private Iterator<Node> curIterator = curBucket.iterator();

            private int findNextEmptyBucket(int cur) {
                int pos = cur;
                while (pos < size && buckets[pos] == null) {
                    pos += 1;
                }
                return pos;
            }

            @Override
            public boolean hasNext() {
                return curIterator.hasNext() || findNextEmptyBucket(pos + 1) < size;
            }

            @Override
            public K next() {
                if (curIterator.hasNext()) {
                    return curIterator.next().key;
                }
                pos = findNextEmptyBucket(pos + 1);
                curBucket = buckets[pos];
                curIterator = curBucket.iterator();
                return curIterator.next().key;
            }
        };
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @Override
    public V get(K key) {
        int index = getIndex(key);
        Collection<Node> curBucket = buckets[index];
        if(curBucket != null) {
            for(Node n : curBucket) {
                if(n.key.equals(key)) {
                    return n.value;
                }
            }
        }
        return null;
    }

    private int getIndex(K key) {
        return Math.floorMod(key.hashCode(), size);
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public void put(K key, V value) {
        if(isOverload()) {
            resize();
        }
        Node cur = createNode(key, value);
        int pos = getIndex(key);
        if(buckets[pos] == null) {
            buckets[pos] = createBucket();
        }
        for(Node n : buckets[pos]) {
            if (n.key.equals(key)) {
                n.value = value;
                return;
            }
        }
        buckets[pos].add(cur);
        length += 1;
    }

    @Override
    public Set<K> keySet() {
        if(length == 0) {
            return null;
        }
        Set<K> res = new HashSet<>();
        for(K k : this) {
            res.add(k);
        }
        return res;
    }

    @Override
    public V remove(K key) {
        int pos = getIndex(key);
        Collection<Node> curbucket = buckets[pos];
        if(curbucket == null) {
            return null;
        }
        for(Node n : curbucket) {
            if(n.key.equals(key)) {
                curbucket.remove(n);
                length -= 1;
                return n.value;
            }
        }
        return null;
    }

    @Override
    public V remove(K key, V value) {
        int pos = getIndex(key);
        Node cur = createNode(key, value);
        Collection<Node> curbucket = buckets[pos];
        if(curbucket == null) {
            return null;
        }
        for(Node n : curbucket) {
            if(n.key.equals(key) && n.value.equals(value)) {
                curbucket.remove(n);
                length -= 1;
                return value;
            }
        }
        return null;
    }

    private boolean isOverload() {
        return (double) length / size > maxLoad;
    }

    private void resize() {
        MyHashMap<K, V> temp = new MyHashMap<>(size * 2, this.maxLoad);
        for (K key : this) {
            temp.put(key, get(key));
        }
        size *= 2;
        buckets = temp.buckets;
    }
}
