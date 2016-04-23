package netty.protobuf.server;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ServerMain {
	public static void main(String[] args) throws InterruptedException {
		try {
			// Create a Parser
			CommandLineParser parser = new BasicParser();
			Options options = new Options();
			options.addOption("h", "help", false, "Print this usage information");
			options.addOption("p", "port", true, "Print out confgure file information");
			// Parse the program arguments
			CommandLine commandLine = parser.parse(options, args);
			// Set the appropriate variables based on supplied option
			if (commandLine.hasOption('h')) {
				System.out.println("Help Message");
				System.exit(0);
			}
			
			int port = 8081;
			if (commandLine.hasOption('p')){
				port =  Integer.parseInt(commandLine.getOptionValue("p"));
			}
			Server svr = new Server();
			svr.start(port);
			svr.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}