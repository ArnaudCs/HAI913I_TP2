package codeanalyser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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


}
