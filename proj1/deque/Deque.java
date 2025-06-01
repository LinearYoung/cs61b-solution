package deque;

public interface Deque <T>{
    //add item to the front of the deque
    void addFirst(T item);

    //add item to the back of the deque
    void addLast(T item);

    //return true if the deque is empty, false otherwise
    default boolean isEmpty(){
        if (size() == 0) {
            return true;
        }
        return false;
    }

    //return the size of the deque
    int size();

    //print the items of the deque,seperated by space
    //if all items have been printed, printed a new line
    void printDeque();

    //Removes and returns the item at the front of the deque. If no such item exists, returns null.
    T removeFirst();

    //Removes and returns the item at the back of the deque. If no such item exists, returns null.
    T removeLast();

    //Gets the item at the given index, where 0 is the front, 1 is the next item, and so forth.
    // If no such item exists, returns null. Must not alter the deque!
    public T get(int index);
}
