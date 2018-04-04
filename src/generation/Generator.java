package generation;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class Generator {
	static int numOfSamplesToGenerate = 10;
	static int maxDerivation = 10;
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost:3306/pcsg";
	static final String USER = "test";
	static final String PASS = "1234";
	static Random rand = new Random();
	static String outputPath = "E:\\xml_gen\\";

	static Map<String, Double> rulesProb = new TreeMap<String, Double>();
	static ArrayList<String> rules = null;

	public static void main(String[] args) {
		Connection conn = null;
		Statement stmt = null;
		String sql = "";
		PrintWriter writer;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("linking to MySQL....");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			stmt = conn.createStatement();
			System.out.println("generating...");
			for (int i = 0; i < numOfSamplesToGenerate; i++) {
				String parent = "null", grandparent = "null", greatparent = "null", sibling = "null";
				String alpha = "DocumentContext";
				System.out.println("===========================" + i + "===========================");
				String sample = derivation(alpha, greatparent, grandparent, parent, sibling, stmt, 0);
				System.out.println(sample);
				writer = new PrintWriter(outputPath + i + ".xml", "UTF-8");
				writer.println(sample);
				writer.close();
			}
			stmt.close();
			conn.close();
		} catch (SQLException se) {
			System.out.println(sql);
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}

		}
	}

	// input: context(), alpha, statement, num
	// output: sample without non-terminal symbols
	static String derivation(String alpha, String greatparent, String grandparent, String parent, String sibling,
			Statement stmt, int num) {
		if (num > maxDerivation) {
			return "";
		}
		String context = greatparent + "," + grandparent + "," + parent + "," + sibling;
		// System.out.println("context:" + context);
		// System.out.println(alpha);
		String result = "";
		String temp = "";
		// 2. get the list of production rules whose left-side is alpha and context
		// matches the current context
		rulesProb.clear();
		rules = new ArrayList<>(rulesProb.keySet());
		rules.clear();
		String sql = "select * from pcsg where parent='" + alpha + "' and context='" + context + "';\n";
		ResultSet rs;
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				rulesProb.put(rs.getString(4), rs.getDouble(5));
				// System.out.print("==================");
				// System.out.print(rs.getString(1) + "\t");
				// System.out.print(rs.getString(2) + "\t");
				// System.out.print(rs.getString(3) + "\t");
				// System.out.println("\t" + rs.getString(4));
				// System.out.print(rs.getString(5) + "\t");
				// System.out.println("==================");
			}
			rules = new ArrayList<>(rulesProb.keySet());
			if (rules.size() == 0) {
				System.out.println("Error: no rules to use. ");
				System.out.println("alpha:" + alpha);
				System.out.println("context:" + context);
			}
			rs.close();
			temp = rules.get(rand.nextInt(rules.size()));
			int leftMark = temp.indexOf("@@@@@");
			int rightMark = 0;
			if (leftMark != -1) {
				result += temp.substring(0, leftMark);
			} else {
				return temp;
			}
			greatparent = grandparent;
			grandparent = parent;
			parent = alpha;
			while (leftMark != -1) {
				rightMark = temp.indexOf("#####", leftMark + 5);
				result += " " + derivation(temp.substring(leftMark + 5, rightMark), greatparent, grandparent, parent,
						sibling, stmt, num + 1);
				leftMark = temp.indexOf("@@@@@", rightMark + 5);
				// System.out.println(leftMark);
				// System.out.println(rightMark);
				if (leftMark != -1) {
					result += " " + temp.substring(rightMark + 5, leftMark);
				} else {
					result += " " + temp.substring(rightMark + 5);
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println(result);
		return result;
	}

}
