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
}
