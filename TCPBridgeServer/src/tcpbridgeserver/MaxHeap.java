package tcpbridgeserver;

import java.util.Vector;

public class MaxHeap {
    private Vector<Double> heap;

    public MaxHeap() {
        heap = new Vector<>();
    }

    private int parent(int i) {
        if (i == 0)
            return 0;

        return (i - 1) / 2;
    }

    private int LEFT(int i) {
        return ( 2 * i + 1);
    }

    private int RIGHT(int i) {
        return (2 * i + 2);
    }

    private void swap(int x, int y) {
        Double temp = heap.get(x);
        heap.setElementAt(heap.get(y), x);
        heap.setElementAt(temp, y);
    }

    private void heapDown(int i) {
        int left = LEFT(i);
        int right = RIGHT(i);

        int largest = i;

        if (left < heap.size() && heap.get(left) > heap.get(i))
            largest = left;

        if (right < heap.size() && heap.get(right) > heap.get(largest))
            largest = right;

        if (largest != i) {
            swap(i, largest);
            heapDown(largest);
        }
    }

    private void heapUp(int i) {
        if (i > 0 && heap.get(parent(i)) < heap.get(i)) {
            swap(i, parent(i));
            heapUp(parent(i));
        }
    }

    public int size() {
        return heap.size();
    }

    public Boolean isEmpty() {
        return heap.isEmpty();
    }

    public void add(Double key) {
        heap.addElement(key);

        int index = size() - 1;
        heapUp(index);
    }

    public Double pull() {
        try {
            if (size() == 0)
                throw new Exception("No elements in heap");

            double root  = heap.firstElement();

            heap.setElementAt(heap.lastElement(), 0);
            heap.remove(size() - 1);

            heapDown(0);
            return root;
        } catch (Exception ex) {
            System.out.println(ex);
            return null;
        }
    }

    public Double peek() {
        try {
            if (size() == 0)
                throw new Exception("No elements in heap");
            return heap.firstElement();
        } catch (Exception ex) {
            System.out.println(ex);
            return null;
        }
    }
}
