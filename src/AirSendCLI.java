import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;

import com.devmel.content.SimpleIPLocator;
import com.devmel.devices.AirSend;
import com.devmel.devices.SimpleIPException;
import com.devmel.rf.Frame;
import com.devmel.rf.Packet;
import com.devmel.tools.Hexadecimal;

import tools.CommandLineParser;
import tools.Pulses;
import tools.SearchQRCode;

public class AirSendCLI {

	public static void main(String[] args) {
		CommandLineParser cli = new CommandLineParser(args);
		SimpleIPLocator deviceInfo = null;
		int datarate = -1;
		int frequency = 433920000;
		boolean frequencyset = false;
		SearchQRCode search = null;

		//Print help
		if(cli == null || cli.hasOption("h") || cli.hasOption("?")){
			help(System.out);
			System.exit(0);
		}

		//Print protocols and commands list
		if(cli.hasOption("l")){
			listProtocolsCommands(System.out);
		}

		//Get device
		if(cli.hasOption("P")){
			try{
				SimpleIPLocator config = new SimpleIPLocator(cli.getOptionValue("P"));
				if(config != null && config.getIp() != null){
					deviceInfo = config;
				}
			}catch(Exception e){}
		}
		if(deviceInfo == null){
			//Scan QR code
			try {
				search = new SearchQRCode();
				if(search.newScan()){
					System.out.println("Scan Started");
					String result = search.waitResult(15000);
					if(result!=null){
						System.out.println("Scanned - "+result);
						deviceInfo = new SimpleIPLocator(result);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("No webcam found...");
			}
		}
		if(deviceInfo == null){
			System.err.println("Scan Failed...");
			System.exit(0);
		}

		if(cli.hasOption("dr")){
			int value = -1;
			try{
				value = Integer.decode(cli.getOptionValue("dr"));
				datarate = (value&0xffff);
			}catch(Exception e){}
		}

		if(cli.hasOption("f")){
			try{
				frequency = Integer.decode(cli.getOptionValue("f"));
				frequencyset = true;
			}catch(Exception e){}
		}

		System.out.println("******************* AirSendCLI *******************");
		long startTime = System.nanoTime();
		deviceInfo.setTimeout(8000);
		AirSend airsend = new AirSend(deviceInfo);
		try {
			if(airsend.open()){
				//Read sensors
				if(cli.hasOption("s")){
					double tmp = airsend.readTemperature();
					int ill = airsend.readIlluminance();
					//Display the result
					if (tmp >= 0) {
						double tmpCelcius = Math.round((tmp - 273.15) * 10.0) / 10.0;
						System.out.println("Temperature : "+String.format("%.1f\u00B0C",tmpCelcius));
					} else if (tmp < 0) {
						System.out.println("Temperature : Error ");
					}
					if(ill >= 0){
						System.out.println("Illuminance : "+ill+" lux");
					}else{
						System.out.println("Illuminance : Error");
					}
				}

				//Read protocols
				if(cli.hasOption("rp")){
					if(datarate <= 0)
						datarate = 7500;
					int lfrequency = 433785000;
					if(frequencyset)
						lfrequency = frequency;
					System.out.println("***** Read protocol : "+lfrequency+" *****");
					byte[] config = AirSend.buildConfigOOK(lfrequency, datarate, true, airsend.getOscillator());
					airsend.setConfiguration(config);
					int duration = 10000;
					final InputStreamReader fileInputStream = new InputStreamReader(System.in);
					try{
						duration = Integer.decode(cli.getOptionValue("rp"));
					}catch(Exception e){}
					long end = System.currentTimeMillis() + duration;
					long lastAlive = System.currentTimeMillis();	//Keep alive
					boolean hasNext = true;
					final Frame f = new Frame();
					boolean resetConf = true;
					int error = 0;
					while(hasNext == true && airsend.open()){
						if(fileInputStream.ready()){
							hasNext = false;
							System.err.println("Terminate");
						}
						if(duration > 1 && end <= System.currentTimeMillis())
							hasNext = false;
						if(resetConf || (lastAlive+60000) < System.currentTimeMillis()){
							airsend.setConfiguration(config);
							resetConf = false;
							lastAlive = System.currentTimeMillis();
						}
						try{
							if(airsend.read(f, hasNext)){
								if(f.getPacketCount() > 0){
									for(int i=0;i<f.getPacketCount();i++){
										Packet p = f.getPacket(i);
										String date = String.format("%1$te/%1$tm/%1$tY %1$tH:%1$tM:%1$tS", new Date());
										System.out.println(date+" : "+Packet.getProtocolName(p.getProtocolId())+" "+p.getSourceAddress()+" "+p.getCommand()+" "+p.getRollingCounter());
									}
								}
								lastAlive = System.currentTimeMillis();
							}
                           	error = 0;
						}catch(SimpleIPException ex){
                            if(ex.getCode() == SimpleIPException.BUSY){
    							System.err.println("Wait, device busy");
                            }else if(ex.getCode() == SimpleIPException.CONNECTION_LOST 
                            		|| ex.getCode() == SimpleIPException.CLOSED 
                            		|| ex.getCode() == SimpleIPException.TIMEOUT){
                            	airsend.close();
                            	airsend = new AirSend(deviceInfo);
                            }else if(ex.getCode() != SimpleIPException.RESPONSE_LOST){
                                throw ex;
                            }else{
                            	error++;
                            }
                        	Thread.sleep(1000);
							resetConf = true;
						}
						if(error > 4){
                        	airsend.close();
                        	airsend = new AirSend(deviceInfo);
							resetConf = true;
						}
					}
				}

				//Write protocols
				if(cli.hasOption("wp")){
					int proto = -1;
					long addr = -1;
					int command = 0;
					int rcode = 0;
					String[] data = null;
					String opt = cli.getOptionValue("wp");
					if(opt != null){
						data = opt.split(":");
					}
					if(data != null && data.length >= 3){
						try{
							if(data.length == 4)
								rcode = Integer.decode(data[3]);
							command = Integer.decode(data[2]);
							addr = Long.decode(data[1]);
							proto = Integer.decode(data[0]);
						}catch(Exception e){}
						if(proto <= 0){
							proto = Packet.getProtocolId(data[0]);
						}
					}
					if(proto>0){
						String rcodeStr = "";
						if(rcode>0)
							rcodeStr = ":"+rcode;
						System.out.println("***** Write protocol : "+frequency+" *****");
						System.out.println(Packet.getProtocolName(proto)+":"+addr+":"+command+rcodeStr);
						Packet packet = new Packet(proto, addr, command, rcode);
						boolean written = airsend.write(new Frame(packet), 0);
						System.out.println("Sent "+written);
					}
				}
				
				//Store protocol put
				if(cli.hasOption("spp")){
					int proto = -1;
					long addr = -1;
					int rcode = 0;
					String[] data = null;
					String opt = cli.getOptionValue("spp");
					if(opt != null){
						data = opt.split(":");
					}
					if(data != null && data.length >= 3){
						try{
							rcode = Integer.decode(data[2]);
							addr = Long.decode(data[1]);
							proto = Integer.decode(data[0]);
						}catch(Exception e){}
						if(proto <= 0){
							proto = Packet.getProtocolId(data[0]);
						}
					}
					if(proto>0){
						System.out.println("***** Store protocol put *****");
						System.out.println(Packet.getProtocolName(proto)+":"+addr+":"+rcode);
						Packet packet = new Packet(proto, addr, 0, rcode);
						boolean result = airsend.storePut(packet);
						System.out.println("Done "+result);
					}
				}
				
				//Store protocol delete
				if(cli.hasOption("spd")){
					int proto = -1;
					long addr = -1;
					String[] data = null;
					String opt = cli.getOptionValue("spd");
					if(opt != null){
						data = opt.split(":");
					}
					if(data != null && data.length >= 2){
						try{
							addr = Long.decode(data[1]);
							proto = Integer.decode(data[0]);
						}catch(Exception e){}
						if(proto <= 0){
							proto = Packet.getProtocolId(data[0]);
						}
					}
					if(proto>0){
						System.out.println("***** Store protocol delete *****");
						System.out.println(Packet.getProtocolName(proto)+":"+addr);
						Packet packet = new Packet(proto, addr, 0, 0);
						boolean result = airsend.storeDel(packet);
						System.out.println("Done "+result);
					}
				}

				//Store protocol list
				if(cli.hasOption("spl")){
					int size = airsend.storeSize();
					System.out.println("***** Store protocol list "+size+"/"+airsend.storeCapacity()+" *****");
					System.out.println("protocol:address:rcode");
					Packet ptmp = new Packet(0,0,0,0);
					for(int i=0;i<size;i++){
						if(airsend.storeGet(i, ptmp)){
							System.out.println(Packet.getProtocolName(ptmp.getProtocolId())+":"+ptmp.getSourceAddress()+":"+ptmp.getRollingCounter());
						}
					}
				}

				//Read raw
				if(cli.hasOption("r")){
					if(datarate <= 0)
						datarate = 3000;
					System.out.println("***** Read raw data : "+frequency+" at "+datarate+"Hz *****");
					byte[] config = AirSend.buildConfigOOK(frequency, datarate, true, airsend.getOscillator());
					boolean resetConf = true;
					airsend.setConfiguration(config);
					int duration = 10000;
					try{
						duration = Integer.decode(cli.getOptionValue("r"));
					}catch(Exception e){}
					final InputStreamReader fileInputStream = new InputStreamReader(System.in);
					long end = System.currentTimeMillis() + duration;
					boolean hasNext = true;
					int error = 0;
					while(hasNext == true && airsend.open()){
						if(fileInputStream.ready()){
							hasNext = false;
							System.err.println("Terminate");
						}
						if(duration > 1 && end <= System.currentTimeMillis())
							hasNext = false;
						if(resetConf){
							airsend.setConfiguration(config);
							resetConf = false;
						}
						try{
							byte[] data = airsend.read(500, hasNext);
							if(data != null)
								System.out.println(Hexadecimal.fromBytes(data));
                           	error = 0;
						}catch(SimpleIPException ex){
                            if(ex.getCode() == SimpleIPException.BUSY){
    							System.err.println("Wait, device busy");
                            }else if(ex.getCode() == SimpleIPException.CONNECTION_LOST 
                            		|| ex.getCode() == SimpleIPException.CLOSED 
                            		|| ex.getCode() == SimpleIPException.TIMEOUT){
                            	airsend.close();
                            	airsend = new AirSend(deviceInfo);
                            }else if(ex.getCode() != SimpleIPException.RESPONSE_LOST){
                                throw ex;
                            }else{
                            	error++;
                            }
                        	Thread.sleep(1000);
							resetConf = true;
						}
						if(error > 4){
                        	airsend.close();
                        	airsend = new AirSend(deviceInfo);
							resetConf = true;
						}
					}
				}
	
				//Write raw
				if(cli.hasOption("w")){
					String data = cli.getOptionValue("w");
					if(data != null){
						if(datarate <= 0)
							datarate = 3000;
						System.out.println("***** Write raw data : "+frequency+" at "+datarate+"Hz *****");
						byte[] rData = null;
						try{
							//Convert from pulses
							String[] pulsesStr = data.replaceAll("^[,\\s]+", "").split("[,\\s]+");
							if(pulsesStr != null && pulsesStr.length > 1){
								int[] pulses = new int[pulsesStr.length];
								for(int i=0;i<pulsesStr.length;i++){
									pulses[i] = Integer.parseInt(pulsesStr[i]);
								}
								rData = Pulses.fromPulses(datarate, pulses, true);
								System.out.println("Pulses : "+Arrays.toString(pulses));
							}
						}catch(Exception ex){}
						if(rData == null){
							try{
								//Convert from hexadecimal
								rData = Hexadecimal.toBytes(data);
							}catch(Exception ex){}
						}
						if(rData != null){
							System.out.println("Data ("+rData.length+" bytes) : "+Hexadecimal.fromBytes(rData));
							byte[] config = AirSend.buildConfigOOK(frequency, datarate, false, airsend.getOscillator());
							airsend.setConfiguration(config);
							int written = airsend.write(rData, false);
							System.out.println("Sent "+((written > 0) ? true : false));
						}

					}
				}
			}else{
				System.err.println("Error : open failed");
			}
		} catch (Exception e) {
			System.err.println("Error : "+e);
		}finally{
			airsend.close();
			if(search != null)
				search.close();
		}
		long estimatedTime = System.nanoTime() - startTime;
		estimatedTime /= 1000000;
		System.out.println("******************* Time "+estimatedTime+" ms ******************");
		System.exit(0);
	}

	public static void listProtocolsCommands(PrintStream out){
		out.println("--------------- Protocols --------------");
		int[] protocols = Packet.getList();
		out.println("ID\t : NAME");
		for(int protocol : protocols){
			out.println(protocol+"\t : "+Packet.getProtocolName(protocol));
		}
		out.println("--------------- Commands ---------------");
		int[] commands = new int[]{Packet.Command_OFF, Packet.Command_ON, Packet.Command_PROG, Packet.Command_STOP, Packet.Command_DOWN, Packet.Command_UP, Packet.Command_TOGGLE};
		out.println("ID\t : NAME");
		for(int command : commands){
			out.println(command+"\t : "+getCommandName(command));
		}
		out.println("----------------------------------------");
	}
	public static String getCommandName(int command){
		String cmd = null;
		switch(command){
			case Packet.Command_OFF:
			cmd = "Command_OFF";
			break;
			case Packet.Command_ON:
			cmd = "Command_ON";
			break;
			case Packet.Command_PROG:
			cmd = "Command_PROG";
			break;
			case Packet.Command_STOP:
			cmd = "Command_STOP";
			break;
			case Packet.Command_DOWN:
			cmd = "Command_DOWN";
			break;
			case Packet.Command_UP:
			cmd = "Command_UP";
			break;
			case Packet.Command_TOGGLE:
			cmd = "Command_TOGGLE";
			break;
		}
		return cmd;
	}

	
	public static void help(PrintStream out){
		out.println("------------------ help ----------------");
		out.println("----------------------------------------");
		out.println("Usage : AirSendCLI [options]");
		out.println("Options :");
		out.println("-P <port>		Connection port (sp://...)");
		out.println("-l			List protocols and commands");
		out.println("-s			Display sensors values");
		out.println("-spl 		Store protocol list");
		out.println("-spp protocol:address:rcode		Store protocol put");
		out.println("-spd protocol:address		Store protocol delete");
		out.println("-wp protocol:address:command_id(:rcode)		Write a protocol command");
		out.println("-rp duration		Read protocol with duration capture in ms");
		out.println("-w data			Write raw data");
		out.println("-r duration		Read raw data with duration capture in ms");
		out.println("-dr datarate		Data rate in Hertz (default : 3000)");
		out.println("-f frequency		Radio Frequency in Hertz (default : 433920000)");
		out.println("-h			Print this help");
	}
}
