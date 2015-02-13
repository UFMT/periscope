package br.ufmt.periscope.util;

import java.util.ArrayList;
import java.util.List;

public class SelectObject<T> {

	private T object;
	private boolean selected = false;
	

	public SelectObject(T obj){
		this.object = obj;
	}
	
	public static <E> List<SelectObject<E>> convertToSelectObjects(List<E> list){
		List<SelectObject<E>> resp = new ArrayList<SelectObject<E>>();
		for(E o : list){
			resp.add(new SelectObject<E>(o));
		}
		return resp;
	}
	
	public T getObject() {
		return object;
	}


	public void setObject(T object) {
		this.object = object;
	}


	public boolean isSelected() {
		return selected;
	}


	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
}
