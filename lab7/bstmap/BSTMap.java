package bstmap;

import java.util.*;
import java.util.function.Consumer;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V>{
    private class BSTNode {
        private K key;
        private V value;
        private BSTNode left;
        private BSTNode right;
        private int size;

        BSTNode(K key, V value, int size) {
            this.key = key;
            this.value = value;
            this.left = null;
            this.right = null;
            this.size = size;
        }
    }

    private BSTNode root;


    @Override
    public void clear() {
        root = null;
    }

    @Override
    public boolean containsKey(K key) {
        return BS(root, key) != null;
    }

    @Override
    public V get(K key) {
        BSTNode keynode = BS(root, key);
        return keynode == null ? null : keynode.value;
    }

    private BSTNode BS(BSTNode node, K key) {
        if(node == null) {
            return null;
        }

        int cmp = key.compareTo(node.key);
        if(cmp > 0) {
            return BS(node.right, key);
        }
        else if(cmp < 0) {
            return BS(node.left, key);
        }
        else {
            return node;
        }
    }

    @Override
    public int size() {
        return size(root);
    }
    private int size(BSTNode node) {
        return node == null ? 0 : node.size;
    }

    @Override
    public void put(K key, V value) {
       root = put(root, key, value);
    }

    private BSTNode put(BSTNode node, K key, V value) {
        if(node == null) {
            node = new BSTNode(key, value, 1);
        }
        else {
            int cmp = key.compareTo(node.key);

            if (cmp > 0) {
                node.right = put(node.right, key, value);
                node.size += 1;
            } else if (cmp < 0) {
                node.left = put(node.left, key, value);
                node.size += 1;
            } else {
                node.value = value;
            }
        }
        return node;
    }

    @Override
    public Set<K> keySet() {
        Set<K> keyset = new HashSet<>();
        LinkedList<BSTNode> nodequece = new LinkedList<>();
        nodequece.addLast(root);
        while (!nodequece.isEmpty()) {
            BSTNode node = nodequece.removeFirst();
            if (node == null) continue;
            keyset.add(node.key);
            nodequece.addLast(node.left);
            nodequece.addLast(node.right);
        }
        return keyset;
    }

    @Override
    public V remove(K key) {
        if (containsKey(key)) {
            V val =  get(key);
            root = remove(root, key);
            return val;
        }
        return null;
    }

    private BSTNode remove(BSTNode node, K key) {
        if(node == null) {
            return null;
        }
        int cmp = key.compareTo(node.key);
        if(cmp > 0) {
            node.right = remove(node.right, key);
        }
        else if (cmp < 0){
            node.left = remove(node.left, key);
        }
        else {
            //有0或1个节点直接将子节点接入该节点的父节点
            if(node.left == null) {
                return node.right;
            }
            if(node.right == null) {
                return node.left;
            }
            //该节点有两个子节点，寻找successor来代替该节点
            //并对右侧部分进行删除successor节点操作，因为successor必然只有0或1个节点，从而实现问题转化
            BSTNode temp = node;
            node = findMin(node.right);
            node.right = removeMin(temp.right);
            node.left = temp.left;
        }
        node.size = 1 + size(node.left) + size(node.right);
        return node;
    }

    private BSTNode findMin(BSTNode node) {
        if(node.left == null) {
            return node;
        }
        else return findMin(node.left);
    }

    private BSTNode removeMin(BSTNode node) {
        if(node.left == null) {
            return node.right;
        }
        node.left = removeMin(node.left);
        node.size = 1 + size(node.left) + size(node.right);
        return node;
    }

    @Override
    public V remove(K key, V value) {
        V current = get(key);
        if(current != null && current.equals(value)) {
            root = remove(root, key);
            return value;
        }
        return null;
    }


    @Override
    public Iterator<K> iterator() {
        return new BSTIterator();
    }

    private class BSTIterator implements Iterator<K> {
        private Deque<BSTNode> stack = new LinkedList<>();


        BSTIterator() {
            BSTNode current = root;
            while(current != null) {
                stack.push(current);
                current = current.left;
            }
        }
        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public K next() {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }
            BSTNode node = stack.pop();
            K res = node.key;
            BSTNode right = node.right;
            while(right != null) {
                stack.push(right);
                right = right.left;
            }
            return res;
        }
    }

    public void printInOrder() {
        printInOrder(root);
    }

    private void printInOrder(BSTNode node) {
        if (node == null) {
            return;
        }
        printInOrder(node.left);
        System.out.print(node.key + ", ");
        printInOrder(node.right);
    }
}
