package codeanalyser;

import java.io.File;import java.text.DecimalFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class CodeAnalyser {
	
	private static Map<String, Map<String, Map<String, String>>> callGraph;
	private static String cmd;
	private static String graphCmd;
	private static Map<List<Double>, List<String>> clusterOrderTree = new HashMap<List<Double>, List<String>>();		
	
	public String getCmd() {return cmd;}
	public String getGraphCmd() {return graphCmd;}

	public static void runAllStats(File folder) {
		// Récupération des fichiers du projet
		ArrayList<File> javaFiles = Parser.listJavaFilesForFolder(folder);

		callGraph = new HashMap<String, Map<String, Map<String, String>>>();
		
		// Loop sur chaque fichier
		for (File fileEntry : javaFiles) {
			String content;
			try {
				// Récupération du contenu du fichier et parsing
				content = FileUtils.readFileToString(fileEntry);
				CompilationUnit parse = Parser.parse(content.toCharArray());
				
				callGraph.putAll(buildCallGraph(parse));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		displayCallGraph(callGraph);
		
		double[][] couplingMatrix = calculateCoupling(callGraph);
		displayCouplingMatrix(couplingMatrix, callGraph.keySet());
		
		
        double[][] weightedGraph = calculateWeightedGraph(couplingMatrix);
        
        List<List<String>> clusters = hierarchicalClustering(callGraph);
        System.out.println("Ordre de clustering : " + clusterOrderTree);
        
        System.out.println("Applications du projet : "+ identifyModules(clusterOrderTree, 0.5));
        System.out.println("\nIdentification de modules avec contraintes (M/2) =========================\n");
        System.out.println("Applications du projet (Avec CP = 0,5) : "+ identifyModulesMin(clusterOrderTree, 0.5, couplingMatrix.length));
	}

	public static void displayCallGraph(Map<String, Map<String, Map<String, String>>> callGraph) {
	    System.out.println("Graphe d'appels (sans méthodes provenants de Java):");
	    graphCmd += "==========================" + "\n";
	    graphCmd += "|      Graphe d'appel      |" + "\n";
	    graphCmd += "==========================" + "\n";

	    for (Map.Entry<String, Map<String, Map<String, String>>> classEntry : callGraph.entrySet()) {
	        String className = classEntry.getKey();
	        Map<String, Map<String, String>> methodCalls = classEntry.getValue();

	        System.out.println("Classe: " + className);
	        graphCmd += "Classe: " + className + "\n";

	        for (Map.Entry<String, Map<String, String>> methodEntry : methodCalls.entrySet()) {
	            String methodName = methodEntry.getKey();
	            Map<String, String> calledMethods = methodEntry.getValue();

	            System.out.println("-> Méthode: " + methodName);
	            graphCmd += "-> Méthode: " + methodName + "\n";

	            if (!calledMethods.isEmpty()) {
	                System.out.println("   Appelle:");
	                graphCmd += "   Appelle:" + "\n";
	                for (Map.Entry<String, String> calledMethodEntry : calledMethods.entrySet()) {
	                    String calledMethodName = calledMethodEntry.getKey();
	                    String declaringClass = calledMethodEntry.getValue();

	                    System.out.println("   -> " + calledMethodName + "  ->  " + declaringClass);
	                    graphCmd += "   -> " + calledMethodName + "  ->  " + declaringClass + "\n";
	                }
	            } else {
	                System.out.println("   Pas d'appel.");
	                graphCmd += "   Pas d'appel." + "\n";
	            }
	        }
	    }
	}

	public static Map<String, Map<String, Map<String, String>>> buildCallGraph(CompilationUnit parse) {
	    ClassDeclarationVisitor classVisitor = new ClassDeclarationVisitor();
	    parse.accept(classVisitor);

	    Map<String, Map<String, Map<String, String>>> callGraph = new HashMap<String, Map<String, Map<String, String>>>();

	    for (TypeDeclaration classDeclaration : classVisitor.getClasses()) {
	        String className = classDeclaration.getName().getIdentifier();
	        Map<String, Map<String, String>> methodCalls = new HashMap<String, Map<String, String>>();

	        MethodDeclarationVisitor methodVisitor = new MethodDeclarationVisitor();
	        classDeclaration.accept(methodVisitor);

	        for (MethodDeclaration methodDeclaration : methodVisitor.getMethods()) {
	            String methodName = methodDeclaration.getName().getIdentifier();
	            Map<String, String> calledMethods = new HashMap<String, String>();

	            MethodInvocationVisitor invocationVisitor = new MethodInvocationVisitor();
	            methodDeclaration.accept(invocationVisitor);

	            for (MethodInvocation methodInvocation : invocationVisitor.getMethods()) {
	                String invokedMethodName = methodInvocation.getName().getIdentifier();
	                IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
	                
	                if (methodBinding != null) {
	                    String declaringClassName = methodBinding.getDeclaringClass().getName();
	                    calledMethods.put(invokedMethodName, declaringClassName);
	                }
	            }

	            methodCalls.put(methodName, calledMethods);
	        }

	        callGraph.put(className, methodCalls);
	    }

	    return callGraph;
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
//	                    	System.out.println(methodA+"-"+methodB);
	                        if (isCoupled(classAMethods.get(methodA), classNameB) || isCoupled(classBMethods.get(methodB), classNameA)) {
	                            couplingCount++;
	                        }
	                    }
	                }

	                // Calculer le couplage relatif
	                int totalRelations = classAMethods.size() + classBMethods.size();
	                double couplingRatio = (double) couplingCount / totalRelations;

	                couplingMatrix[i][j] = couplingRatio;
	            }
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
	    graphCmd += "==========================" + "\n";
	    graphCmd += "|   Tableau de couplage  |" + "\n";
	    graphCmd += "==========================" + "\n";
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
	            System.out.printf("%.2f\t", couplingMatrix[i][j]);
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
    
    public static List<List<String>> hierarchicalClustering(Map<String, Map<String, Map<String, String>>> callGraph) {
        // Récupérer la liste des classes
        String[] classNames = callGraph.keySet().toArray(new String[0]);
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
                    double coupling = calculateAverageCoupling(clusters.get(i), clusters.get(j), callGraph);
                    //System.out.println("Coupling average de : " + clusters.get(i) + " et " + clusters.get(j) + " est de : " + coupling);
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
    
    public static double calculateAverageCoupling(List<String> cluster1, List<String> cluster2, Map<String, Map<String, Map<String, String>>> callGraph) {
        int totalCouplingCount = 0;
        int totalRelationsCount = 0;
        for (String className1 : cluster1) {
            for (String className2 : cluster2) {
                if (!className1.equals(className2)) {
                    Map<String, Map<String, String>> class1Methods = callGraph.get(className1);
                    Map<String, Map<String, String>> class2Methods = callGraph.get(className2);
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
                    totalRelationsCount += class1Methods.size() + class2Methods.size();
                }
            }
        }
        return (double) totalCouplingCount / totalRelationsCount;
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
                return Double.compare(secondElement1, secondElement2); // Trie en ordre croissant
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
}

