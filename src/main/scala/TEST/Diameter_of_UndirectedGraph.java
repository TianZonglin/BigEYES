package TEST;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Diameter_of_UndirectedGraph {

    // method to read file and return the lines of the file as  List<String>
    public static List<String> readFile(String filename) {

        List<String> records = new ArrayList<String>();
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Diameter_of_UndirectedGraph.class.getResourceAsStream(filename)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.equals("")) {

                    } else {
                        records.add(line);
                    }

                }
                reader.close();
            }
            return records;
        } catch (IOException e) {
            System.err.format("Exception occurred trying to read '%s'.", filename);
            return null;
        }

    }


    public static int calculateDiameter(String wordslistfile){
        List<String> lines = readFile(wordslistfile);

        Graph G = new Graph(lines.size() * 2);

        List<String> vertices = new ArrayList<>();
        int edges = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if( line.contains(" "))
            {
                edges++;

            }
            System.out.println(line);
            String[] split = line.split(" ");
            if(split.length != 2)
            {
                continue;
            }

            if (!vertices.contains(split[0].trim())) {
                vertices.add(split[0].trim());
            }
            if (!vertices.contains(split[1].trim())) {
                vertices.add(split[1].trim());
            }

            G.addEdge(vertices.indexOf(split[0].trim()), vertices.indexOf(split[1].trim()));
            G.addEdge(vertices.indexOf(split[1].trim()), vertices.indexOf(split[0].trim()));

        };

        // end


        //Code to calculate the Diameter of the undirected graph
        int diameter = 0;


        // If graph is disconnected, its diameter should be Integer.MAX_VALUE

        if(edges < (vertices.size()-1)){
            diameter = Integer.MAX_VALUE;
        }else{
            // We first find the shortest path between each pair of vertices.
            for (int i = 0; i < vertices.size() - 1; i++) {
                for (int j = i + 1; j < vertices.size(); j++) {
                    LinkedList<Integer> shortest = G.getShortestPath(i, j);
                    // The greatest length of any of these paths is the diameter of the graph.
                    if ((shortest.size() - 1) > diameter) {
                        diameter = shortest.size() - 1;
                    }
                }
            }
        }

        //  System.out.println("Diameter of the undirected graph is " + diameter);
        return diameter;

    }

    public static void main(String[] args) {

        // Code for reading the file and string the vertices and creating graph
        String wordslistfile = "I:\\IDEA_PROJ\\Visualization\\src\\main\\scala\\temp.csv";
        System.out.println("Diameter of the undirected graph is " + calculateDiameter(wordslistfile));
    }

}