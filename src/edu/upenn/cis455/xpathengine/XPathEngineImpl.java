package edu.upenn.cis455.xpathengine;

import java.util.*;
import java.util.regex.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathEngineImpl implements XPathEngine {

	private String[] xpaths; // Array that stores the passed xpaths
	private int number_of_xpaths; // Number of xpaths passed 
	private boolean[] results; // Array that stores the result of each of the xpaths
	private boolean[] valids; // Array that stores the validity of each of the xpaths

	public XPathEngineImpl() {

	}

	/**
	 * Function to pass the xpaths
	 */
	public void setXPaths(String[] s) {
		xpaths = s;
		number_of_xpaths = xpaths.length;
		valids = new boolean[number_of_xpaths];
		results = new boolean[number_of_xpaths];
		for (int i=0; i<number_of_xpaths; i++) { // Initialize the arrays
			valids[i] = false;
			results[i] = false;
		}
	}

	/**
	 * Function that evaluates the array of xpaths against the document d
	 */
	public boolean[] evaluate(Document d) { 
		getvalids();
		for (int i = 0; i<number_of_xpaths; i++) {
			if (valids[i]) // If the xpath is valid, determine whether it matches the document or not
				results[i] = evaluate_xpath(xpaths[i], d);
			else // If the xpath is invalid, return false
				results[i] = false;
		}
		return results; 
	}

	/**
	 * Function that returns the boolean array holding the indices of all the valid xpaths
	 * @return
	 */
	public boolean[] getvalids() {
		for (int i=0; i<xpaths.length; i++) {
			if (isValid(i))
				valids[i] = true;
			else
				valids[i] = false;
		}
		return valids;
	}

	/**
	 * Check if the i-th xpath in the xpaths array is valid or not
	 */
	public boolean isValid(int i) {
		String xpath = xpaths[i];
		if (!xpath.startsWith("/")) { // If the xpath does not start with a '/', it is invalid according to our grammar
			return false;
		}
		if (xpath.replace("/", "").equals("")) { // If only '/'s are present in the whole xpath, eg: ////
			return false;
		}
		String[] xpath_parts = split_xpath(xpath); // Get all the steps in the xpath
		for (String xpath_part : xpath_parts) {
			if (xpath_part.equals(""))
				continue;
			if (!check_step(xpath_part)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This function is used to extract the steps from the xpath
	 * @param xpath is the xpath string
	 * @return a string array containing all the steps of the xpath
	 */
	private String[] split_xpath(String xpath) {
		ArrayList<String> parts = new ArrayList<String>(); 
		char[] xpath_array = xpath.toCharArray();
		String part = "";
		int inside_bracket = 0;
		int start;
		if (xpath.startsWith("/"))
			start = 1;
		else
			start = 0;
		for (int i=start; i<xpath_array.length; i++) {
			if (xpath_array[i] == '[')
				inside_bracket++;
			if (xpath_array[i] == ']')
				inside_bracket--;
			if (xpath_array[i] == '/' && (inside_bracket == 0)) { // The '/' here is a step seperator, eg: a/b
				parts.add(part); // Add the step to the array
				part = "";
				continue;
			}
			part += xpath_array[i]; // If not, the '/' is part of the step (specifically the filter), eg: [a/b]
		}
		parts.add(part); // Add the last step of the xpath to our steps array
		String[] xpath_parts = new String[parts.size()];
		xpath_parts = parts.toArray(xpath_parts);
		return xpath_parts;
	}

	/**
	 * Function that evaluates a single xpath against the document d
	 * @param xpath is the xpath to be evaluated
	 * @param d is the document to be evaluated against
	 * @return true if the xpath matches the document and false otherwise
	 */
	private boolean evaluate_xpath(String xpath, Document d) {
		Node root = d.getDocumentElement();
		ArrayList<Node> candidate_nodes = new ArrayList<Node>();
		candidate_nodes.add(root);
		String [] steps = split_xpath(xpath);
		for (String step : steps) { // Evaluate each of the steps in a top-down order
			candidate_nodes = evaluate_step(step, candidate_nodes);
			if (!(candidate_nodes.size()>0)) { // The step does not match the doc, hence return false
				return false;
			}
		}
		return true; // Return true if all the steps matched the doc
	}

	/**
	 * Evaluate if a particular step of an xpath is satisfied or not
	 * @param step represents the step to be evaluated
	 * @param candidate_nodes is the list of nodes in the DOM tree (level) where we need to start checking
	 * @return The list of nodes that match the step
	 */
	private ArrayList<Node> evaluate_step(String step, ArrayList<Node> candidate_nodes) {
		ArrayList<Node> next_candidate_nodes = new ArrayList<Node>(); // The candidate nodes for the next step
		String nodename = ""; // The nodename part of the step
		if (!step.contains("[") && !step.contains("/")) // If no filter exists, then the step is just a nodename
			nodename = step;
		else if (slash_before_bracket(step)) { // eg: a/b[c]
			nodename = step.split("/",2)[0];
		}
		else
			nodename = step.split("\\[")[0].trim(); // eg: a[b/c]
		for (Node node : candidate_nodes) {
			if (node.getNodeName().equals(nodename)) { // The candidate node that matches the nodename of the step
				if (!step.contains("/") && !step.contains("[")) {
					NodeList children = node.getChildNodes();
					for (int i=0; i<children.getLength(); i++) { // Add its children to the list of next candidate nodes
						next_candidate_nodes.add(children.item(i));
					}
				}
				else if (slash_before_bracket(step)) { // Nested xpath filter
					String[] step_parts = step.split("\\/",2);
					NodeList new_candidate_nodes = node.getChildNodes();
					ArrayList<Node> list = new ArrayList<Node>();
					for (int i=0; i<new_candidate_nodes.getLength(); i++) {
						list.add(new_candidate_nodes.item(i));
					}
					ArrayList<Node> ret_list = evaluate_step(step_parts[1], list);
					if ((ret_list.size() > 0)) {
						for (Node child_node : list) 
							next_candidate_nodes.add(child_node);
					}
				}
				else if (test_step(step, node)) { // If all the tests in the step pass
					NodeList children = node.getChildNodes();
					for (int i=0; i<children.getLength(); i++) { // Add its children to the list of next candidate nodes
						next_candidate_nodes.add(children.item(i));
					}
				}
			}
		}
		return next_candidate_nodes;
	}

	/**
	 * Check if a path seperator exists in a step or not thus indicating whether or not it is a nested xpath filter
	 * @param step
	 * @return true if a path seperator exists
	 */
	private boolean slash_before_bracket(String step) {
		if (!step.contains("/"))
			return false;
		if (step.contains("/") && !step.contains("["))
			return true;
		char[] step_array = step.toCharArray();
		boolean bracket_found = false;
		for (char ch : step_array) {
			if (ch == '[') 
				bracket_found = true;
			if (ch == '/') {
				if (!bracket_found) 
					return true;
				else
					return false;
			}
		}
		return false;
	}

	/**
	 * Check if it is a nodename according to the grammar
	 * @param str is the string to check whether or not its a filter
	 * @return true it is nodename or false otherwise
	 */
	private boolean check_if_nodename(String str) {
		String regex = "^[a-zA-Z_:]+[a-zA-Z0-9_\\-:.\\/]*";
		if (str.matches(regex)) {
			if (str.toLowerCase().startsWith("xml")) // Nodename should not begin with "xml"
				return false;
			return true;
		}
		else
			return false;
	}

	/**
	 * Check if the filter is of the form text()=""
	 * @param str is the filter
	 * @return true if it is of this form and false otherwise
	 */
	private boolean check_if_step1(String str) {
		String regex = "\\s*text\\s*\\(\\s*\\)\\s*=\\s*\"(.*?)\"\\s*";
		if (str.matches(regex))
			return true;
		else
			return false;
	}

	/**
	 * Check if the filter is of the form @attname=""
	 * @param str is the filter
	 * @return true if it is of this form and false otherwise
	 */
	private boolean check_if_step2(String str) {
		String regex = "\\s*@\\s*([a-zA-Z_:]+[a-zA-Z0-9_\\-\\/]*)\\s*=\\s*\"(.*?)\"\\s*";
		if (str.matches(regex))
			return true;
		else
			return false;
	}

	/**
	 * Check if the filter is of the form contains(text(),"")
	 * @param str is the filter
	 * @return true if it is of this form and false otherwise
	 */
	private boolean check_if_step3(String str) {
		String regex = "\\s*contains\\s*\\(\\s*text\\s*\\(\\s*\\)\\s*\\,\\s*\"(.*?)\"\\s*\\)\\s*";
		if (str.matches(regex))
			return true;
		else
			return false;
	}

	/**
	 * Check if the step is valid
	 * @param str is the step
	 * @return true if it is a valid step and false otherwise
	 */
	private boolean check_step(String str) {
		if (!str.contains("[")) {
			if (!check_if_nodename(str))
				return false;
			else
				return true;
		}
		int counter = 0; // To keep track of the number of open and close brackets
		boolean first_bracket = true;
		boolean inside_quote = false;
		char[] str_chars = str.toCharArray();
		String str_part = "";
		for (int i=0; i<str_chars.length; i++) {
			if (str_chars[i] == ' ') {
				if (inside_quote) {
					str_part += str_chars[i];
					continue;
				}
				else {
					continue;
				}
			}
			if (str_chars[i] == '"') {
				str_part += str_chars[i];
				inside_quote = !inside_quote;
				continue;
			}
			if (str_chars[i] == '[') {
				if (inside_quote) {
					str_part += str_chars[i];
					continue;
				}
				counter++;
				if (first_bracket) {
					if (str_part.equals("")) { //xyz/[test()]
						return false;
					}
					if (!check_if_nodename(str_part)) {
						return false;
					}
					first_bracket = false;
					str_part = "";
				}
				else {
					if (!str_part.equals("") && !inside_quote) {
						String str_part_new = str_part.replace("/", "");
						if (!check_if_nodename(str_part_new)) {
							return false;
						}
						first_bracket = false;
						str_part = "";
					}
				}
			}
			else if (str_chars[i] == ']') {
				if (inside_quote) {
					str_part += str_chars[i];
					continue;
				}
				counter--;
				if (str_part.equals("")) //]]
					continue;
				if (check_if_nodename(str_part) || check_if_step1(str_part) || check_if_step2(str_part) || check_if_step3(str_part)) {
					str_part = "";
				}
				else {
					return false;
				}
			}
			else {
				str_part += str_chars[i];
			}
		}
		if (counter!=0) { // Mismatch of open and close brackets
			return false;
		}
		return true;
	}

	/**
	 * Function to test if a step satisfies all the filters
	 * @param step is the step to be tested
	 * @param node
	 * @return true if the step passes all the tests
	 */
	private boolean test_step(String step, Node node) {
		HashMap<String, String> filters = get_filters(step);
		NamedNodeMap attributes = node.getAttributes();
		if (filters.containsKey("text_filter")) { // Validate text filter
			boolean ret_bool = false;
			NodeList child_nodes = node.getChildNodes();
			for (int i=0; i<child_nodes.getLength(); i++) {
				Short type = child_nodes.item(i).getNodeType(); // Get the type of the child node
				if (type.toString().equals("3")) { // Text nodes have type 3
					if (child_nodes.item(i).getNodeValue().equals(filters.get("text_filter")))
						ret_bool = true;
				}
			}
			if (!ret_bool)
				return false;
		}
		for (String key : filters.keySet()) {
			if (key.startsWith("contains_text_filter")) {
				boolean ret_bool = false;
				NodeList child_nodes = node.getChildNodes();
				for (int i=0; i<child_nodes.getLength(); i++) {
					Short type = child_nodes.item(i).getNodeType();
					if (type.toString().equals("3")) {
						if (child_nodes.item(i).getNodeValue().contains(filters.get(key)))
							ret_bool = true;
					}
				}
				if (!ret_bool)
					return false;
			}
			else if (key.startsWith("attr_filter")) { // Validate all the attribute filters
				String attr_filter = filters.get(key);
				String[] attr_filter_parts = attr_filter.split("::");
				if (!attributes.getNamedItem(attr_filter_parts[0]).getNodeValue().equals(attr_filter_parts[1]))
					return false;
			}
		}
		// We reach here only if the above filters passed
		ArrayList<Node> new_candidate_nodes = new ArrayList<Node>();
		NodeList list = node.getChildNodes();
		for (int i=0; i<list.getLength(); i++) {
			new_candidate_nodes.add(list.item(i));
		}
		for (String key : filters.keySet()) { // Validate all the nested xpath filters
			if (key.startsWith("nested_filter")) {
				String nested_filter = filters.get(key);
				ArrayList<Node> child_nodes = evaluate_step(nested_filter, new_candidate_nodes);
				if (!(child_nodes.size() > 0)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Function that gets all the filters present in a single step
	 * @param main_filter is the step
	 * @return a hashmap containing key as the type of the filter and value as the filter itself
	 */
	private HashMap<String, String> get_filters(String main_filter) {
		HashMap<String, String> filters_hash = new HashMap<String, String>();
		String str_pattern_text1 = "\\s*text\\s*\\(\\s*\\)\\s*=\\s*\"(.*?)\"\\s*";
		String str_pattern_text2 = "\\s*contains\\s*\\(\\s*text\\s*\\(\\s*\\)\\s*\\,\\s*\"(.*?)\"\\s*\\)\\s*";
		String str_pattern_attr = "\\s*@\\s*([a-zA-Z_:]+[a-zA-Z0-9_\\-\\/]*)\\s*=\\s*\"(.*?)\"\\s*";
		Pattern pattern_text1 = Pattern.compile(str_pattern_text1);
		Pattern pattern_text2 = Pattern.compile(str_pattern_text2);
		Pattern pattern_attr = Pattern.compile(str_pattern_attr);
		Matcher matcher;
		ArrayList<String> filters = new ArrayList<String>();
		char[] filter_char_array = main_filter.toCharArray();
		String filter = "";
		int counter = 0;
		for (char ch : filter_char_array) {
			if (ch == '[') {
				if (counter == 0) {
					filter = "";
				}
				if (counter > 0) {
					filter += ch;
				}
				counter++;
			}
			else if (ch == ']') {
				counter--;
				if (counter == 0) {
					filters.add(filter);
					filter = "";
				}
				else
					filter += ch;
			}
			else
				filter += ch;
		}
		for (String each_filter : filters) {
			boolean found = false;
			int index = 0;
			if (each_filter.contains("[")) {
				filters_hash.put("nested_filter_"+index, each_filter.substring(0,each_filter.length()));
				index++;
			}
			else {
				matcher = pattern_text1.matcher(each_filter);
				while (matcher.find()) {
					found = true;
					if (filters_hash.containsKey("text_filter")) {
						// error if two text filters are present?
						// System.err.println("Looks like two text filters are present!");
					}
					else
						filters_hash.put("text_filter", matcher.group(1).replace("\\\"", "\""));
				}
				matcher = pattern_text2.matcher(each_filter);
				while (matcher.find()) {
					found = true;
					filters_hash.put("contains_text_filter_"+filters.indexOf(each_filter), matcher.group(1).replace("\\\"", "\""));
				}
				matcher = pattern_attr.matcher(each_filter);
				while (matcher.find()) {
					found = true;
					filters_hash.put("attr_filter_"+filters.indexOf(each_filter), matcher.group(1)+"::"+matcher.group(2));
				}
				if (!found) {
					filters_hash.put("nested_filter_"+index, each_filter);
					index++;
				}
			}
		}
		return filters_hash;
	}

}