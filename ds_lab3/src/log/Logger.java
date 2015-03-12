package log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;

import clock.ClockService;

import util.TimeStampedMessage;
import core.MessagePasser;


public class Logger{
	private static ArrayList<TimeStampedMessage> sortedList;
	public static void main(String[] args) throws IOException{
		if (args.length != 3 || !args[1].equalsIgnoreCase("logger")){
			System.err.println("Usage: $java -cp jar_file log/logger <config_file> logger <clockType>");
			System.exit(1);
		}
		MessagePasser mp = new MessagePasser(args[0], args[1], args[2]);
		mp.listen();
		
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader buffedReader = new BufferedReader(input);
		
		while(true){
			System.out.println("Logger is running");
			System.out.println("Type in show to see the log, or exit to quit the logger");
			String line = buffedReader.readLine().trim();
			if (line.equalsIgnoreCase("show")){
				show(mp.getRecivedQueue());
			}
			else if (line.equalsIgnoreCase("exit")){
				System.exit(1);
			}
			else{
				System.err.println("Invalid Input");
			}
		}
	}
	
	private static void show(ConcurrentLinkedQueue<TimeStampedMessage> receive_queue){
		sortedList = new ArrayList<TimeStampedMessage>(receive_queue);
		Collections.sort(sortedList, new Comparator<TimeStampedMessage>(){
			@Override
			public int compare(TimeStampedMessage o1, TimeStampedMessage o2) {
				ClockService c1 = o1.getClock();
				ClockService c2 = o2.getClock();
				int rst = c1.compareTo(c2);
				if (rst == 0){
					o1.getConcurrent().add(o2);
					o2.getConcurrent().add(o1);
				}
				return rst;
			}});
		System.out.println("Logged Messages:");
		for (TimeStampedMessage m : sortedList){
			System.out.println(m);
		}
	}
}
