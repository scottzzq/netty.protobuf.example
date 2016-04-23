package netty.protobuf.client;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netty.protobuf.NettyConstant;

public class ClientMain {
	private static Logger logger = LoggerFactory.getLogger(ClientMain.class.getName());
	public static void main(String[] args) throws Exception {
		// Create a Parser
		CommandLineParser parser = new BasicParser();
		Options options = new Options();
		options.addOption("h", "help", false, "Print this usage information");
		options.addOption("t", "thread", true, "Print out Thread information");
		options.addOption("H", "host", true, "Host information");
		options.addOption("P", "port", true, "Port information");
		// Parse the program arguments
		CommandLine commandLine = parser.parse(options, args);
		// Set the appropriate variables based on supplied option
		if (commandLine.hasOption('h')) {
			System.out.println("Help Message");
			System.exit(0);
		}
		
		int thread_num = 1;
		if (commandLine.hasOption('t')) {
			thread_num = Integer.parseInt(commandLine.getOptionValue('t'));
		}
		
		String host = "127.0.0.1";
		if (commandLine.hasOption("H")){
			host = commandLine.getOptionValue('H');
		}
		
		int port = 8088;
		if (commandLine.hasOption("P")){
			port = Integer.parseInt(commandLine.getOptionValue('P'));
		}
		
		final String h = host;
		final int p = port;
		Thread threads[] = new Thread[thread_num];
		for (int i = 0; i < thread_num; ++i) {
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						new Client().connect(h, p);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			threads[i].start();
			logger.info("Start Connecting Thread");
		}
		for (int i = 0; i < thread_num; ++i) {
			threads[i].join();
		}
	}
}