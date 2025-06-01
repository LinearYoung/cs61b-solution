package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Deque<T>, Iterable<T>{
     private int size;
     private T[] items;
     private int first;
     private int last;
     private double usagefactor;

     private class ArrayDequeIterator implements Iterator<T> {
         private int pos = 0;

         @Override
         public boolean hasNext() {
             return pos < size;
         }

         @Override
         public T next() {
             if(! hasNext()) {
                 return null;
             }
             T current = get(pos);
             pos += 1;
             return current;
         }
     }
     public ArrayDeque() {
         items = (T[])new Object[8];
         size = 0;
         first = 0;
         last = 0;
         usagefactor = 0.25;
     }

     private void resize(int capacity) {
         T[] newArray = (T[]) new Object[capacity];
         for (int i = 0; i < size; i++) {
             newArray[i] = items[(first + i) % items.length];
         }
         items = newArray;
         first = 0;
         last = size == 0 ? 0 : size - 1;
     }
    @Override
    public void addFirst(T item) {
        if(size == items.length) {
            resize(size * 2);
        }
        if(size != 0) {
            first = (first + items.length - 1) % items.length;
        }
        items[first] = item;
        size += 1;
    }

    @Override
    public void addLast(T item) {
         if(size == items.length) {
             resize(size * 2);
         }
         if(size != 0) {
             last = (last + 1) % items.length;
         }
         items[last] = item;
         size += 1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public T get(int index) {
         if(index >= size) {
             return null;
         }
        return items[(index + first) % items.length];
    }

    @Override
    public T removeLast() {
         if(size == 0) {
             return null;
         }
         T LastItem = items[last];
         items[last] = null;
         if(size != 1) {
             last = (last + items.length - 1) % items.length;
         }
        size -= 1;
         if(size >= 16 && size < items.length * usagefactor) {
             resize (items.length / 2);
         }
        return LastItem;
    }

    @Override
    public T removeFirst() {
         if(size == 0) {
             return null;
         }
         T FirstItem = items[first];
         items[first] = null;
         if(size != 1) {
             first = (first + 1) % items.length;
         }
         size -= 1;
        if(size >= 16 && size < items.length * usagefactor) {
            resize (items.length / 2);
        }
        return FirstItem;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    @Override
    public void printDeque() {
        for(int i = 0; i < size; i ++) {
            System.out.print(items[(i + first) % items.length] + " ");
        }
        System.out.println();
    }

    public boolean equals(Object o) {
         if(o == this) return true;
         if(o instanceof Deque) {
             Deque<T> target = (Deque<T>) o;
             for(int i = 0; i < size; i ++) {
                 if(!this.get(i).equals(target.get(i))) {
                     return false;
                 }
             }
         } else {
             return false;
         }
         return true;
    }
}
