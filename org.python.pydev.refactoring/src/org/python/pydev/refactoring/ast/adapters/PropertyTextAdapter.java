package org.python.pydev.refactoring.ast.adapters;

public class PropertyTextAdapter extends TextNodeAdapter {
	
	public static final int GETTER = 0;
	public static final int SETTER = 1;
	public static final int DELETE = 2;
	public static final int DOCSTRING = 3;
	
	private int type;

	public PropertyTextAdapter(int type, String name) {
		super(name);
		this.type = type;
	}

	public int getType() {
		return type;
	}
	
	

}
