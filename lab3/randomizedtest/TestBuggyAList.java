package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import timingtest.AList;

import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
  // YOUR TESTS HERE
  @Test
    public void testThreeAddThreeRemove() {
    AListNoResizing<Integer> test1 = new AListNoResizing<>();
    BuggyAList<Integer> test2 = new BuggyAList<>();
    test1.addLast(1);
    test1.addLast(2);
    test1.addLast(3);
    test2.addLast(1);
    test2.addLast(2);
    test2.addLast(3);

    assertEquals(test1.size(), test2.size());
    assertEquals(test1.removeLast(), test2.removeLast());
    assertEquals(test1.removeLast(), test2.removeLast());
    assertEquals(test1.removeLast(), test2.removeLast());
  }
  @Test
    public void randomizedTest() {
    AListNoResizing<Integer> L = new AListNoResizing<>();
    BuggyAList<Integer> BugL = new BuggyAList<>();

    int N = 5000;
    for (int i = 0; i < N; i += 1) {
      int operationNumber = StdRandom.uniform(0, 3);
      if (operationNumber == 0) {
        // addLast
        int randVal = StdRandom.uniform(0, 100);
        L.addLast(randVal);
        BugL.addLast(randVal);
        System.out.println("addLast(" + randVal + ")");
      }
      else if(L.size() > 0) {
          if (operationNumber == 1) {
          assertEquals(L.getLast(), BugL.getLast());
        } else if (operationNumber == 2) {
          assertEquals(L.removeLast(), BugL.removeLast());
        }
      }
    }
  }
}
