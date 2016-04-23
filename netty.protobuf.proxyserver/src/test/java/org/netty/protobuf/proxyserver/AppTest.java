package org.netty.protobuf.proxyserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	/**
	 * Rigourous Test :-)
	 */
	public void test1() {
		int LEVEL_NUM = 100;
		int port = 8081;
		System.out.println("LEVEL_NUM=" + LEVEL_NUM);
		for (int level = 0; level < LEVEL_NUM; ++level) {
			System.out.println("LEVEL_" + level + "_NUM=1");
			System.out.println("LEVEL_"+ level + "_IP_0=10.126.90.15$"+ port++);
		}
	}
	
	public void test2() throws IOException{
		Properties pro = new Properties();
		FileInputStream in = new FileInputStream("D:\\Users\\58\\workspace\\netty.protobuf.example\\netty.protobuf.proxyserver\\src\\main\\resources\\server.conf");
		pro.load(in);
		System.out.println(pro.get("LEVEL_NUM"));
		in.close();
	}
}
