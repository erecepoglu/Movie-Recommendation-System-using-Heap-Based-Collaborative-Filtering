package movierecommender;

import java.util.ArrayList;

public class MaxHeap {
    private ArrayList<HeapNode> heap = new ArrayList<>();

    public void insert(HeapNode node) {
        heap.add(node);
        heapifyUp(heap.size() - 1);
    }

    public HeapNode extractMax() {
        if (heap.isEmpty()) return null;
        HeapNode max = heap.get(0);
        HeapNode last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            heapifyDown(0);
        }
        return max;
    }

    public boolean isEmpty() { return heap.isEmpty(); }
    public int size() { return heap.size(); }

    private void heapifyUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (heap.get(index).similarity <= heap.get(parent).similarity) break;
            swap(index, parent);
            index = parent;
        }
    }

    private void heapifyDown(int index) {
        while (true) {
            int left = 2 * index + 1;
            int right = 2 * index + 2;
            int largest = index;

            if (left < heap.size() && heap.get(left).similarity > heap.get(largest).similarity) largest = left;
            if (right < heap.size() && heap.get(right).similarity > heap.get(largest).similarity) largest = right;

            if (largest == index) break;
            swap(index, largest);
            index = largest;
        }
    }

    private void swap(int i, int j) {
        HeapNode temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
}
