package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T>, Iterable<T> {
    private class Node<T> {
        private Node<T> prev = null;
        private T item;
        private Node<T>next = null;

        Node (T i) {
            item = i;
        }
        //getRecursive help method
        public T getRecursive(int i) {
            if(i == 0) {
                return item;
            }
            else {
                assert next != null;
                return next.getRecursive(i - 1);
            }
        }
    }
    private class LinkedListDequeIterator implements Iterator<T> {
        private int pos = 0;

        @Override
        public boolean hasNext() {
            return pos < size;
        }

        @Override
        public T next() {
            if(!hasNext()) {
                return null;
            }
            T current = get(pos);
            pos += 1;
            return current;
        }
    }
    private Node<T> sentinel;
    private int size;

    @Override
    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
    }

    public LinkedListDeque() {
        sentinel = new Node<>(null);
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        size = 0;
    }

    @Override
    public void addFirst(T item) {
        Node n = new Node(item);
        size += 1;
        n.next = sentinel.next;
        sentinel.next.prev = n;
        sentinel.next = n;
        n.prev = sentinel;
    }

    @Override
    public void addLast(T item) {
        Node n = new Node(item);
        size += 1;
        n.prev = sentinel.prev;
        sentinel.prev.next = n;
        sentinel.prev = n;
        n.next = sentinel;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        Node current = sentinel.next;
        while(current != sentinel) {
            System.out.print(current.item + " ");
            current = current.next;
        }
        System.out.println();
    }

    @Override
    public T removeFirst() {
        if(size == 0) {
            return null;
        }
        size -= 1;
        Node<T> first = sentinel.next;
        first.next.prev = sentinel;
        sentinel.next = first.next;
        return first.item;
    }

    @Override
    public T removeLast() {
        if (size == 0) {
            return null;
        }
        size -= 1;
        Node<T> last = sentinel.prev;
        sentinel.prev =last.prev;
        last.prev.next = sentinel;
        return last.item;
    }

    @Override
    public T get(int index) {
        if(index >= size) {
            return null;
        }
        Node<T> current = sentinel.next;
        for(int i = 0; i < index; i ++) {
            current = current.next;
        }
        return current.item;
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }
        if(o instanceof Deque) {
            Deque<T> target = (Deque<T>) o;
            if(target.size() != size) {
                return false;
            }
            for(int i = 0; i < size; i ++) {
                if(target.get(i) != this.get(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    public T getRecursive(int index) {
        if (index >= size) {
            return null;
        }
        return sentinel.next.getRecursive(index);
    }
}
