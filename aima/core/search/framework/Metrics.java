package aima.core.search.framework;

import java.util.Hashtable;
import java.util.Set;

/**
 * Stores key-value pairs for efficiency analysis.
 * @author Ravi Mohan
 * @author Ruediger Lunde
 */
public class Metrics {
	private final Hashtable<String, String> hash;

	public Metrics() {
		this.hash = new Hashtable<>();
	}

	public void set(String name, int i) {
		hash.put(name, Integer.toString(i));
	}

	public void set(String name, double d) {
		hash.put(name, Double.toString(d));
	}
	
	public void set(String name, long l) {
		hash.put(name, Long.toString(l));
	}
        
        public void set(String name, String s) {
		hash.put(name, s);
	}

	public int getInt(String name) {
		return Integer.parseInt(hash.get(name));
	}

	public double getDouble(String name) {
		return Double.parseDouble(hash.get(name));
	}
	
	public long getLong(String name) {
		return Long.parseLong(hash.get(name));
	}

	public String get(String name) {
		return hash.get(name);
	}
        
        public Hashtable<String, String> getAll() {
		return hash;
	}

	public Set<String> keySet() {
		return hash.keySet();
	}
	
        @Override
	public String toString() {
		return hash.toString();
	}
}
