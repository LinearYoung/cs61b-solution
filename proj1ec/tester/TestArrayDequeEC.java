package tester;

import static org.junit.Assert.*;
import org.junit.Test;
import student.StudentArrayDeque;
import edu.princeton.cs.algs4.StdRandom;

public class TestArrayDequeEC {
   @Test
    public void test1() {
       String message = "";
       StudentArrayDeque<Integer> deque1 = new StudentArrayDeque<Integer>();
       ArrayDequeSolution<Integer> deque2 = new ArrayDequeSolution<Integer>();
        for(int i = 0; i < 10000; i ++) {
            int opt = StdRandom.uniform(0, 4);
            if(opt == 0) {
                int num = StdRandom.uniform(0, 100);
                message += "addFirst(" + num + ")\n";
                deque1.addFirst(num);
                deque2.addFirst(num);
            }
            if(opt == 1) {
                int num = StdRandom.uniform(0, 100);
                message += "addLast(" + num + ")\n";
                deque1.addLast(num);
                deque2.addLast(num);
            }

            if(deque1.isEmpty()) {
                continue;
            }
            if(opt == 2) {
                message += "removeFirst()\n";
                assertEquals(message,deque2.removeFirst(),deque1.removeFirst());
            }
            if(opt == 3) {
                message += "removeLast()\n";
                assertEquals(message,deque2.removeLast(),deque1.removeLast());
            }
        }
   }
}
