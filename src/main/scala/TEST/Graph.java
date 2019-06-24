
package TEST;



import java.util.Iterator;
import java.util.LinkedList;

public class Graph {


    private int V;
    private LinkedList<Integer> adjacent[];

    Graph(int v) {
        V = v;
        adjacent = new LinkedList[v];

        for (int i = 0; i < v; i++) {
            adjacent[i] = new LinkedList<Integer>();
        }
    }

    void addEdge(int v1, int v2) {
        adjacent[v1].add(v2);
    }

    LinkedList<Integer> getShortestPath(int start, int end) {
        LinkedList<Integer> queue = new LinkedList<Integer>();
        LinkedList<Integer> result = new LinkedList<Integer>();

        int previous[] = new int[V];

        if (start == end) {
            return result;
        }


        queue.add(start);

        for (int i = 0; i < V; i++) {
            previous[i] = -1;
        }

        while (!queue.isEmpty()) {
            int current = queue.poll();
            Iterator<Integer> i = adjacent[current].listIterator();



            while (i.hasNext()) {
                int n = i.next();

                if (previous[n] == -1) {
                    previous[n] = current;

                    if (n == end) {
                        while (n != start) {
                            result.addFirst(n);
                            n = previous[n];
                        }

                        result.addFirst(start);
                        return result;
                    }

                    queue.add(n);
                }
            }
        }


        return result;
    }

}
