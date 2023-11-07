package spoon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;

public class CodeAnalyzer {
	private static Map<List<Double>, List<String>> clusterOrderTree;
	private static Map<String, Map<String, Map<String, String>>> newCallGraph;
	static int totalRelation = 0;

    public static void main(String[] args) {
        String projectPath = "/home/e20190003865/Bureau/HAI913I_TP2/test_coupling/";

        // Create a Spoon launcher
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(8);
        launcher.addInputResource(projectPath);

        // Build the model
        CtModel model = launcher.buildModel();

        // Create a data structure to store the call graph
        Map<String, Map<String, List<String>>> callGraph = new HashMap<>();
        newCallGraph = new HashMap<String, Map<String, Map<String, String>>>();
        

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
        
        
        newCallGraph = transformCallGraph(callGraph);
        System.out.println("GRAPHE: "+ newCallGraph);
        double[][] couplingMatrix = calculateCoupling(newCallGraph);
		displayCouplingMatrix(couplingMatrix, newCallGraph.keySet());
		
		List<List<String>> clusters = hierarchicalClustering(newCallGraph);
        System.out.println("Ordre de clustering : " + clusterOrderTree);
        
        System.out.println("Applications du projet : "+ identifyModules(clusterOrderTree, 0.1));
        //System.out.println("\nIdentification de modules avec contraintes (M/2) =========================\n");
        System.out.println("Applications du projet (Avec CP = 0,5) : "+ identifyModulesMin(clusterOrderTree, 0.1, couplingMatrix.length));
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
    
    
	public static double[][] calculateCoupling(Map<String, Map<String, Map<String, String>>> callGraph) {
	    // Récupérer la liste des classes
	    String[] classNames = callGraph.keySet().toArray(new String[0]);
	    int numClasses = classNames.length;

	    // Initialiser la matrice de couplage
	    double[][] couplingMatrix = new double[numClasses][numClasses];

	    // Remplir la matrice de couplage
	    for (int i = 0; i < numClasses; i++) {
	        for (int j = 0; j < numClasses; j++) {
	            if (i == j) {
	                // Ignorer la diagonale (couplage d'une classe à elle-même)
	                couplingMatrix[i][j] = 0.0;
	            } else {
	                String classNameA = classNames[i];
	                String classNameB = classNames[j];

	                Map<String, Map<String, String>> classAMethods = callGraph.get(classNameA);
	                Map<String, Map<String, String>> classBMethods = callGraph.get(classNameB);

	                int couplingCount = 0;

	                // Compter le nombre d'appels de A à B et de B à A
	                for (String methodA : classAMethods.keySet()) {
	                    for (String methodB : classBMethods.keySet()) {
	                        if (isCoupled(classAMethods.get(methodA), classNameB)) {
	                            couplingCount++;
	                        }
	                    }
	                }
	                
	                totalRelation += couplingCount;


	                couplingMatrix[i][j] = couplingCount;
	            }
	        }
	    }
	    
	    System.out.println(totalRelation);
	    
	    
	    for(int i = 0; i < couplingMatrix.length; i++) {
	    	for(int j = 0; j < couplingMatrix.length; j++) {
	    		couplingMatrix[i][j] = couplingMatrix[i][j]/totalRelation;
	    	}
	    }
	    
	    return couplingMatrix;
	}

	private static boolean isCoupled(Map<String, String> calledMethods, String className) {
	    for (String methodName : calledMethods.keySet()) {
//	    	System.out.println(calledMethods.get(methodName)+"-"+className);
	        if (calledMethods.get(methodName).equals(className)) {
	            return true;
	        }
	    }
	    return false;
	}
	
	public static void displayCouplingMatrix(double[][] couplingMatrix, Set<String> classNamesSet) {
		System.out.println("\nTableau de couplage (sans méthodes provenants de Java):");
	    int numClasses = classNamesSet.size();
	    String[] classNames = classNamesSet.toArray(new String[classNamesSet.size()]);
	    // Print the header row with class names
	    System.out.print("\t");
	    for (int i = 0; i < numClasses; i++) {
	        System.out.print(classNames[i] + "\t");
	    }
	    System.out.println();

	    // Print the coupling matrix
	    for (int i = 0; i < numClasses; i++) {
	        System.out.print(classNames[i] + "\t");
	        for (int j = 0; j < numClasses; j++) {
	            System.out.printf("%.3f\t", couplingMatrix[i][j]);
	        }
	        System.out.println();
	    }
	}

    private static double[][] calculateWeightedGraph(double[][] couplingMatrix) {
        int numClasses = couplingMatrix.length;
        double[][] weightedGraph = new double[numClasses][numClasses];

        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                if (i == j) {
                    // No coupling to itself
                    weightedGraph[i][j] = 0.0;
                } else {
                    // Calculate coupling strength based on the coupling matrix
                    double totalCoupling = 0.0;
                    for (int k = 0; k < numClasses; k++) {
                        totalCoupling += couplingMatrix[i][k] + couplingMatrix[j][k];
                    }
                    weightedGraph[i][j] = totalCoupling / (2 * numClasses);
                }
            }
        }

        return weightedGraph;
    }
    
    //Clustering dendro
    
    public static List<List<String>> hierarchicalClustering(Map<String, Map<String, Map<String, String>>> newCallGraph) {
        // Récupérer la liste des classes
        String[] classNames = newCallGraph.keySet().toArray(new String[0]);
        int numClasses = classNames.length;

        List<List<String>> clusters = new ArrayList<List<String>>();
        
        // Initialisation : chaque classe est un cluster
        for (String className : classNames) {
            List<String> initialCluster = new ArrayList<String>();
            initialCluster.add(className);
            clusters.add(initialCluster);
        }

        while (clusters.size() > 1) {
            // Trouver les deux clusters les plus couplés
            int cluster1Index = -1;
            int cluster2Index = -1;
            double minCoupling = Double.MIN_VALUE;

            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                	//On cherche les clusters les plus couplés pour commencer l'algorithme dendro
                    double coupling = calculateAverageCoupling(clusters.get(i), clusters.get(j), newCallGraph);
//                    System.out.println("Coupling average de : " + clusters.get(i) + " et " + clusters.get(j) + " est de : " + coupling);
                    if (coupling > minCoupling) {
                        minCoupling = coupling;
                        cluster1Index = i;
                        cluster2Index = j;
                    }
                }
            }

            // Fusionner les deux clusters en un nouveau cluster (ligne C3 = cluster(C1, C2);
            if (cluster1Index != -1 && cluster2Index != -1) {
                // Fusionner les deux clusters en un nouveau cluster (ligne C3 = cluster(C1, C2);
                List<String> mergedCluster = mergeClusters(clusters.get(cluster1Index), clusters.get(cluster2Index));
                clusters.remove(cluster1Index);
                if (cluster2Index > cluster1Index) {
                    cluster2Index--; // Décrémenter l'index si nécessaire
                }
                clusters.remove(cluster2Index);
                clusters.add(mergedCluster);
                List<Double> listIndexCoupling = new ArrayList<Double>();
                listIndexCoupling.add((double) clusters.size());
                // Limiter à deux chiffres après la virgule
                minCoupling = Math.round(minCoupling * 100.0) / 100.0;
                listIndexCoupling.add(minCoupling);
                clusterOrderTree.put(listIndexCoupling, mergedCluster);
            }
        }

        return clusters;
    }
    
    public static List<String> mergeClusters(List<String> cluster1, List<String> cluster2) {
        List<String> mergedCluster = new ArrayList<String>();
        mergedCluster.addAll(cluster1);
        mergedCluster.addAll(cluster2);
        return mergedCluster;
    }
    
    public static double calculateAverageCoupling(List<String> cluster1, List<String> cluster2, Map<String, Map<String, Map<String, String>>> newCallGraph) {
        int totalCouplingCount = 0;
        int totalRelationsCount = 0;
        int totalRelations = 0;
        for (String className1 : cluster1) {
            for (String className2 : cluster2) {
                if (!className1.equals(className2)) {
                    Map<String, Map<String, String>> class1Methods = newCallGraph.get(className1);
                    Map<String, Map<String, String>> class2Methods = newCallGraph.get(className2);
                    int couplingCount = 0;
                    // Compter le nombre d'appels de A à B et de B à A
                    for (String methodA : class1Methods.keySet()) {
                        for (String methodB : class2Methods.keySet()) {
                            if (isCoupled(class1Methods.get(methodA), className2) || isCoupled(class2Methods.get(methodB), className1)) {
                                couplingCount++;
                            }
                        }
                    }
                    totalCouplingCount += couplingCount;
                    totalRelationsCount ++;
                }
            }
        }
        
        return (( (double) totalCouplingCount / (double) totalRelationsCount) / (double) totalRelation);
    }
    
    public static Map<List<Double>, List<String>> identifyModules(Map<List<Double>, List<String>> clusterMap, Double CP) {
    	Map<List<Double>, List<String>> applications = new HashMap<List<Double>, List<String>>();
    	
    	for (List<Double> couplingValue: clusterMap.keySet()) {
			if(couplingValue.get(1) >= CP) {
				applications.put(couplingValue, clusterMap.get(couplingValue));
			}
		}
    	return applications;
    }
    
    public static Map<List<Double>, List<String>> identifyModulesMin(Map<List<Double>, List<String>> clusterMap, Double CP, int classCount) {
        List<Map.Entry<List<Double>, List<String>>> sortedEntries = new ArrayList<Entry<List<Double>, List<String>>>(clusterMap.entrySet());

        // Trier la liste d'entrées en utilisant Collections.sort() avec un comparateur spécifique
        Collections.sort(sortedEntries, new Comparator<Map.Entry<List<Double>, List<String>>>() {
            public int compare(Map.Entry<List<Double>, List<String>> entry1, Map.Entry<List<Double>, List<String>> entry2) {
                Double secondElement1 = entry1.getKey().get(1);
                Double secondElement2 = entry2.getKey().get(1);
                return Double.compare(secondElement2, secondElement1); // Trie en ordre croissant
            }
        });
        
        Map<List<Double>, List<String>> applications = new HashMap<List<Double>, List<String>>();


        // Sélectionner les M premiers éléments de la liste triée
        for (int i = 0; i <= classCount/2 && i < sortedEntries.size(); i++) {
            Map.Entry<List<Double>, List<String>> entry = sortedEntries.get(i);
            if (entry.getKey().get(1) >= CP) {
                applications.put(entry.getKey(), entry.getValue());
            }
        }

        return applications;
    }
    
    public static Map<String, Map<String, Map<String, String>>> transformCallGraph(Map<String, Map<String, List<String>>> actualCallGraph) {
    	Map<String, Map<String, Map<String, String>>> fctCallGraph = new HashMap<String, Map<String, Map<String, String>>>();
    	for (Map.Entry<String, Map<String, List<String>>> classEntry : actualCallGraph.entrySet()) {
    	    String className = classEntry.getKey().split("\\.")[1];
    	    Map<String, List<String>> methodCalls = classEntry.getValue();
    	    // Create a new map for the class's methods and their calls
    	    Map<String, Map<String, String>> classMethods = new HashMap<>();

    	    // Iterate through the methods and their calls
    	    for (Map.Entry<String, List<String>> methodEntry : methodCalls.entrySet()) {
    	        String methodName = methodEntry.getKey();
    	        List<String> calledMethods = methodEntry.getValue();
    	        // Create a new map for the method's calls
    	        Map<String, String> methodCallsMap = new HashMap<>();

    	        // Split the original format ("declaringClass.calledMethod") into class and method
    	        for (String calledMethod : calledMethods) {
    	            String[] parts = calledMethod.split("\\.");
    	            
    	            if (parts.length == 3) {
    	                String declaringClass = parts[1];
    	                String calledMethodName = parts[2];
    	                methodCallsMap.put(calledMethodName, declaringClass);
    	            }
    	        }

    	        classMethods.put(methodName, methodCallsMap);
    	    }

    	    fctCallGraph.put(className, classMethods);
    	}
    	return fctCallGraph;
    }
}


