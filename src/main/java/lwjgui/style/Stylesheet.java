package lwjgui.style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import lwjgui.scene.Node;

public class Stylesheet {
	private String source;
	private HashMap<String, StyleSelector> idToStyleSelector = new HashMap<>();
	private HashMap<StyleSelector, StyleData> styleData = new HashMap<>();

	public Stylesheet(String css) {
		this.source = css;
	}
	
	public void applyStyling(Node node) {
		// Apply styling for the DOM TAG
		applyStyling(node, StyleSelectorType.TAG, node.getElementType());
		
		// Apply styling for the class
		ArrayList<String> classList = node.getClassList();
		for (int i = 0; i < classList.size(); i++) {
			String claz = classList.get(i);
			applyStyling(node, StyleSelectorType.CLASS, claz);
		}
	}
	
	public void applyStyling(Node node, String forceElementType) {
		applyStyling(node, StyleSelectorType.TAG, forceElementType);
	}

	private boolean applyStyling(Node node, StyleSelectorType type, String id) {
		StyleSelector selector = idToStyleSelector.get(id);
		if ( selector == null )
			return false;
		
		if ( selector.getType() != type )
			return false;
		
		StyleData data = styleData.get(selector);
		if ( data == null )
			return false;
		
		// Apply the styling!
		applyStyling(node, data, "normal");
		if ( node.isHover() )
			applyStyling(node, data, "hover");
		if ( node.isSelected() )
			applyStyling(node, data, "focus");
		if ( node.isClicked() )
			applyStyling(node, data, "active");
		
		return true;
	}
	
	private void applyStyling(Node node, StyleData data, String methodType) {
		List<StyleOperationValue> operations = data.getStyleOperations(methodType);
		if ( operations.size() <= 0 )
			return;
		
		for (int i = 0; i < operations.size(); i++) {
			operations.get(i).process(node);
		}
	}

	public void compile() {
		StringBuilder currentSelector = new StringBuilder();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);

			if (c == '{') {
				List<StyleSelector> selectors = parseSelectors(currentSelector.toString());
				currentSelector.setLength(0);
				if (selectors == null)
					continue;

				StringBuilder content = new StringBuilder();
				for (int j = i; j < source.length(); j++) {
					char cc = source.charAt(j);

					if (cc == '}') {
						// Parse content
						parseContent(selectors, content.toString());
						i = j;
						break;
					} else {
						content.append(cc);
					}
				}
			} else {
				// Read selector string
				currentSelector.append(c);
			}
		}
	}

	/**
	 * Parse all content for a selector
	 * @param selectors
	 * @param content
	 */
	private void parseContent(List<StyleSelector> selectors, String content) {
		System.out.println("Found selectors (" + selectors.size() + "): " + Arrays.toString(selectors.toArray()));

		HashMap<Object, StyleVarArgs> data = new HashMap<>();

		String currentKey = null;
		StringBuilder t = new StringBuilder();
		for (int i = 1; i < content.length(); i++) {
			char c = content.charAt(i);

			// Store key
			if (c == ':') {
				currentKey = t.toString().trim();
				t.setLength(0);
				continue;
			}

			// End key
			if (c == ';') {
				String currentVal = t.toString().trim();
				StyleVarArgs val = parseArgs(currentVal);
				if ( val != null )
					data.put(currentKey, val);
				currentKey = null;
				t.setLength(0);
				continue;
			}

			// Continue reading
			t.append(c);
		}
		
		// In case there was an unfinished key...
		if ( t.length() > 0 && currentKey != null ) {
			String currentVal = t.toString().trim();
			StyleVarArgs val = parseArgs(currentVal);
			if ( val != null )
				data.put(currentKey, val);
			t.setLength(0);
		}
		
		for (int i = 0; i < selectors.size(); i++) {
			StyleSelector selector = selectors.get(i);
			StyleSelector key = idToStyleSelector.get(selector.selector);
			if ( key == null ) {
				idToStyleSelector.put(selector.selector, selector);
				key = selector;
			}
			
			StyleData sData = styleData.get(key);
			if ( sData == null ) {
				sData = new StyleData();
				styleData.put(selector, sData);
			}
			

			List<StyleOperationValue> operations = sData.getStyleOperations(selector.getModifier());
			if ( operations == null )
				return;
			data.entrySet().forEach(entry -> {
				System.out.println(selector.selector + " :: '" + entry.getKey() + "' = '" + entry.getValue() + "'");
				StyleOperation op = StyleOperations.match(entry.getKey().toString());
				
				if ( op != null ) {
					StyleOperationValue opValue = new StyleOperationValue(op, entry.getValue());
					operations.add(opValue);
				}
			});
		}
	}
	
	/**
	 * Take a css value and parse it into a list of args
	 * @param content
	 * @return
	 */
	private StyleVarArgs parseArgs(String content) {
		ArrayList<Object> temp = new ArrayList<Object>();
		String current = "";
		boolean inFunction = false;
		StyleFunction sFunc = null;
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);
			
			if ( (c == ' ' || i+1 == content.length()) && !inFunction ) {
				if ( i+1 == content.length() && !inFunction )
					current += c;
				
				String t = current.trim();
				current = "";
				Object o = parseVal(t);
				if ( o != null ) {
					temp.add(o);
				}
			} else {
				if ( c == '(' ) {
					inFunction = true;
					sFunc = new StyleFunction(current.trim());
					current = "";
					continue;
				} else if ( c == ')' ) {
					inFunction = false;
					StyleVarArgs argFunc = parseArgs(current.trim().replace(" ", "").replace(",", " "));
					if ( argFunc.size() > 0 ) {
						sFunc.args = argFunc;
						temp.add(sFunc);
						sFunc = null;
					}
					current = "";
					continue;
				} else {
					current = current + c;
				}
			}
		}
		
		Object[] objs = temp.toArray(new Object[temp.size()]);
		return new StyleVarArgs(objs);
	}

	/**
	 * Try to parse string to a value (number, percent, string).
	 * 
	 * @param value
	 * @return
	 */
	private Object parseVal(String value) {
		Object t = parseNumber(value);
		if (t != null)
			return t;

		t = parsePercent(value);
		if (t != null)
			return t;

		return value;
	}

	/**
	 * Try to parse string to number.
	 * 
	 * @param value
	 * @return
	 */
	private Number parseNumber(String value) {
		if (!value.endsWith("px"))
			return null;
		
		value = value.substring(0,value.length()-2);
		try {
			Number t = Double.parseDouble(value);
			return t;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Try to parse string to percentage.
	 * 
	 * @param value
	 * @return
	 */
	private Percentage parsePercent(String value) {
		if (!value.endsWith("%"))
			return null;

		value = value.substring(0, value.length() - 1);
		Number num = parseNumber(value + "px");
		if (num == null)
			return null;

		return new Percentage(num.doubleValue());
	}

	/**
	 * Turn string of selectors into list.
	 * @param selectorString
	 * @return
	 */
	private List<StyleSelector> parseSelectors(String selectorString) {
		List<StyleSelector> ret = new ArrayList<>();
		selectorString = selectorString.trim();
		String[] t = selectorString.contains(",") ? selectorString.split("\\,") : new String[] { selectorString };
		for (int i = 0; i < t.length; i++) {
			String t2 = t[i].trim();
			ret.add(new StyleSelector(t2));
		}

		return ret;
	}
	
	class StyleFunction {
		protected StyleVarArgs args;
		private String name;
		
		public StyleFunction(String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
		
		public StyleVarArgs getArgs() {
			return this.args;
		}
		
		@Override
		public String toString() {
			return name + "(" + args + ")";
		}
	}

	class StyleData {

		private HashMap<String, List<StyleOperationValue>> routines = new HashMap<>();
		
		public StyleData() {
			this.routines.put("normal", new ArrayList<>());
			this.routines.put("hover", new ArrayList<>());
			this.routines.put("active", new ArrayList<>());
			this.routines.put("focus", new ArrayList<>());
		}
		
		public List<StyleOperationValue> getStyleOperations(String modifier) {
			return routines.get(modifier);
		}
	}

	class Percentage {
		private double percent;

		public Percentage(double percent) {
			this.percent = percent;
		}

		public double getPercent() {
			return percent;
		}

		public double getValue() {
			return percent / 100d;
		}
		
		public String toString() {
			return percent + "%";
		}
	}

	class StyleSelector {
		private String selector;
		private StyleSelectorType type;
		private String modifier = "normal"; // Useful for :hover support

		public StyleSelector(String selector) {
			// Parse out event
			if ( selector.contains(":") ) {
				String[] t = selector.split(":");
				selector = t[0];
				this.modifier = t[1];
			}
			
			// Parse out class
			if ( selector.startsWith(".") ) {
				selector = selector.substring(1);
				type = StyleSelectorType.CLASS;
			} else {
				type = StyleSelectorType.TAG;
			}
				
			// Set selector
			this.selector = selector;
		}
		
		public String getModifier() {
			return this.modifier;
		}

		public StyleSelectorType getType() {
			return this.type;
		}

		public String getSelector() {
			return this.selector;
		}

		@Override
		public String toString() {
			return getSelector();
		}
		
		public int hashCode() {
			return selector.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if ( !(o instanceof StyleSelector) )
				return false;
			
			StyleSelector s = (StyleSelector)o;
			if ( !s.selector.contentEquals(selector) )
				return false;
			
			return true;
		}
	}

	enum StyleSelectorType {
		TAG, CLASS;
	}
}

class StyleVarArgs {
	private ArrayList<Object> values = new ArrayList<Object>();
	
	public StyleVarArgs(Object...objects) {
		for (int i = 0; i < objects.length; i++) {
			values.add(objects[i]);
		}
	}
	
	public int size() {
		return values.size();
	}
	
	public Object get(int index) {
		return values.get(index);
	}
	
	@Override
	public String toString() {
		return Arrays.toString(values.toArray(new Object[values.size()]));
	}
}

abstract class StyleOperation {
	public StyleOperation(String key) {
		StyleOperations.operations.put(key, this);
	}

	public abstract void process(Node node, StyleVarArgs value);
}

class StyleOperationValue {
	private StyleOperation operation;
	private StyleVarArgs value;
	
	public StyleOperationValue(StyleOperation operation, StyleVarArgs value) {
		this.value = value;
		this.operation = operation;
	}
	
	public void process(Node node) {
		operation.process(node, value);
	}
}
