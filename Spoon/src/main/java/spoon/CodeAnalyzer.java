package spoon;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;

public class CodeAnalyzer {
	private static Map<List<Double>, List<String>> clusterOrderTree;

    public static void main(String[] args) {
        String projectPath = "/home/gecko/Desktop/HAI913I_TP2/test_coupling/src";

        // Create a Spoon launcher
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(8);
        launcher.addInputResource(projectPath);

        // Build the model
        CtModel model = launcher.buildModel();

        // Create a data structure to store the call graph
        Map<String, Map<String, List<String>>> callGraph = new HashMap<>();

        // Analyze the model
        for (CtType<?> ctClass : model.getAllTypes()) {
            Map<String, List<String>> methodCalls = new HashMap<>();
            for (CtMethod<?> method : ctClass.getMethods()) {
                List<CtExecutableReference<?>> calls = method.getElements(new CallFilter());
                List<String> calledMethods = new ArrayList<>();
                for (CtExecutableReference<?> call : calls) {
                    String declaringClass = call.getDeclaringType().getQualifiedName();
                    String calledMethod = call.getSimpleName();
                    if (!declaringClass.startsWith("java")) {
                        calledMethods.add(declaringClass + "." + calledMethod);
                    }
                }
                methodCalls.put(method.getSimpleName(), calledMethods);
            }
            callGraph.put(ctClass.getQualifiedName(), methodCalls);
        }

        // Print the call graph
        for (Map.Entry<String, Map<String, List<String>>> entry : callGraph.entrySet()) {
            String className = entry.getKey();
            Map<String, List<String>> methodCalls = entry.getValue();
            System.out.println("Class: " + className);
            for (Map.Entry<String, List<String>> methodEntry : methodCalls.entrySet()) {
                String methodName = methodEntry.getKey();
                List<String> calledMethods = methodEntry.getValue();
                System.out.println("  Method: " + methodName);
                for (String calledMethod : calledMethods) {
                    System.out.println("    Calls: " + calledMethod);
                }
            }
        }
        
        double[][] couplingMatrix = calculateCouplingFromCallGraph(callGraph);
        printCouplingBetweenClasses(callGraph, couplingMatrix);
        
        // Cluster the classes
        clusterOrderTree = new HashMap<List<Double>, List<String>>();

        List<List<String>> clusters = hierarchicalClustering(callGraph);
        System.out.println("Ordre de clustering : " + clusterOrderTree);
    }

    // Custom filter to identify method calls
    static class CallFilter extends TypeFilter<CtExecutableReference<?>> {
        CallFilter() {
            super(CtExecutableReference.class);
        }

        @Override
        public boolean matches(CtExecutableReference<?> reference) {
            return !reference.isStatic() && reference.getDeclaringType() != null;
        }
    }
    
    
    public static double[][] calculateCouplingFromCallGraph(Map<String, Map<String, List<String>>> callGraph) {
        // Get the list of classes
        String[] classNames = callGraph.keySet().toArray(new String[0]);
        int numClasses = classNames.length;

        // Initialize the coupling matrix
        double[][] couplingMatrix = new double[numClasses][numClasses];

        // Fill the coupling matrix
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                if (i == j) {
                    // Ignore the diagonal (coupling of a class to itself)
                    couplingMatrix[i][j] = 0.0;
                } else {
                    String classNameA = classNames[i];
                    String classNameB = classNames[j];

                    Map<String, List<String>> classAMethods = callGraph.get(classNameA);
                    Map<String, List<String>> classBMethods = callGraph.get(classNameB);

                    int couplingCount = 0;

                    // Count the number of calls from A to B and from B to A
                    for (String methodA : classAMethods.keySet()) {
                        for (String methodB : classBMethods.keySet()) {
                            if (isCoupled(classAMethods.get(methodA), classNameB) || isCoupled(classBMethods.get(methodB), classNameA)) {
                                couplingCount++;
                            }
                        }
                    }

                    // Calculate the coupling ratio
                    int totalRelations = classAMethods.size() + classBMethods.size();
                    double couplingRatio = (double) couplingCount / totalRelations;

                    couplingMatrix[i][j] = couplingRatio;
                }
            }
        }

        return couplingMatrix;
    }

    private static boolean isCoupled(List<String> calledMethods, String className) {
        for (String calledMethod : calledMethods) {
            if (calledMethod.startsWith(className)) {
                return true;
            }
        }
        return false;
    }

    
    public static void printMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.printf("%.2f\t", matrix[i][j]);
            }
            System.out.println();
        }
    }

    public static void printCouplingBetweenClasses(Map<String, Map<String, List<String>>> callGraph, double[][] couplingMatrix) {
        String[] classNames = callGraph.keySet().toArray(new String[0]);

        System.out.println("Coupling between classes:");
        for (int i = 0; i < classNames.length; i++) {
            for (int j = i + 1; j < classNames.length; j++) {
                String classA = classNames[i];
                String classB = classNames[j];
                double coupling = couplingMatrix[i][j];
                System.out.println(classA + " <-> " + classB + ": " + coupling);
            }
        }
    }

    
    public static List<List<String>> hierarchicalClustering(Map<String, Map<String, List<String>>> callGraph) {
        String[] classNames = callGraph.keySet().toArray(new String[0]);
        int numClasses = classNames.length;

        List<List<String>> clusters = new ArrayList<>();

        // Initialize: each class is a cluster
        for (String className : classNames) {
            List<String> initialCluster = new ArrayList<>();
            initialCluster.add(className);
            clusters.add(initialCluster);
        }

        while (clusters.size() > 1) {
            int cluster1Index = -1;
            int cluster2Index = -1;
            double minCoupling = Double.MIN_VALUE;

            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double coupling = calculateAverageCoupling(clusters.get(i), clusters.get(j), callGraph);
                    if (coupling > minCoupling) {
                        minCoupling = coupling;
                        cluster1Index = i;
                        cluster2Index = j;
                    }
                }
            }

            if (cluster1Index != -1 && cluster2Index != -1) {
                List<String> mergedCluster = mergeClusters(clusters.get(cluster1Index), clusters.get(cluster2Index));
                clusters.remove(cluster1Index);
                if (cluster2Index > cluster1Index) {
                    cluster2Index--; // Decrement the index if necessary
                }
                clusters.remove(cluster2Index);
                clusters.add(mergedCluster);
            }
        }

        return clusters;
    }

    public static double calculateAverageCoupling(List<String> cluster1, List<String> cluster2, Map<String, Map<String, List<String>>> callGraph) {
        int totalCouplingCount = 0;
        int totalRelationsCount = 0;

        for (String className1 : cluster1) {
            for (String className2 : cluster2) {
                if (!className1.equals(className2)) {
                    Map<String, List<String>> class1Methods = callGraph.get(className1);
                    Map<String, List<String>> class2Methods = callGraph.get(className2);
                    int couplingCount = 0;

                    for (String methodA : class1Methods.keySet()) {
                        for (String methodB : class2Methods.keySet()) {
                            if (isCoupled(class1Methods.get(methodA), className2) || isCoupled(class2Methods.get(methodB), className1)) {
                                couplingCount++;
                            }
                        }
                    }

                    totalCouplingCount += couplingCount;
                    totalRelationsCount += class1Methods.size() + class2Methods.size();
                }
            }
        }

        return (double) totalCouplingCount / totalRelationsCount;
    }


    private static List<String> mergeClusters(List<String> cluster1, List<String> cluster2) {
        List<String> mergedCluster = new ArrayList<>(cluster1);
        mergedCluster.addAll(cluster2);
        return mergedCluster;
    }


}


