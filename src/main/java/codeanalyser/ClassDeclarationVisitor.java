package codeanalyser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ClassDeclarationVisitor extends ASTVisitor {
	private List<TypeDeclaration> classes = new ArrayList<TypeDeclaration>();
	private int numberOfClass = 0;
	
	public boolean visit(TypeDeclaration node) {
		if(!node.isInterface()) {
			classes.add(node);
			numberOfClass++;
		}
		return super.visit(node);
	}
	
	public List<TypeDeclaration> getClasses() {
		return classes;
	}
	
	public int getNumberOfClass() {
		return numberOfClass;
	}
}
