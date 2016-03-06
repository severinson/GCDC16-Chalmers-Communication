import java.nio.ByteBuffer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.Scanner;
import java.util.Hashtable;

import java.net.UnknownHostException;

public class VaTestSuite{
    private static DatagramSocket send;
    private static DatagramSocket receive;
    
    //static String VEHICLE_ADAPTER_IP = "192.168.188.136";
    static String VEHICLE_ADAPTER_IP = "127.0.0.1";
    static int VEHICLE_ADAPTER_UDP_PORT = 5000;
    static int RECEIVE_PORT = 5001;
    static int MAX_UDP_SIZE = 2000;
    static int CAM_RATE = 25;
    static int DENM_RATE = 10;
    static int ICLCM_RATE = 25;
    static int verbosity = 2;
    static int STATION_ID = 100;
    static int NUM_VEHICLES = 1;
    
    /* Data for sending */
    static byte[] camData;
    static byte[] denmData;
    static byte[] iclcmData;

    /* Data for receiving */
    static byte[] camRecData;
    static byte[] denmRecData;
    static byte[] iclcmRecData;

    private static class CamService implements Runnable{
        public void run(){
            System.out.println("[CAM] Starting service at " + CAM_RATE + "Hz!");
            ByteBuffer camBuffer = ByteBuffer.wrap(camData);
            int generationDeltaTime = camBuffer.getInt(5);
            int station_id_offset = 0;
            while(camData != null){
                camBuffer.putInt(5,generationDeltaTime);
                camBuffer.putInt(1,STATION_ID+station_id_offset);
                DatagramPacket packet;
                camData = camBuffer.array();
                try{
                    packet =
                        new DatagramPacket(camData, camData.length,
                                           InetAddress.getByName(VEHICLE_ADAPTER_IP),
                                           VEHICLE_ADAPTER_UDP_PORT);
                }catch(UnknownHostException e){
                    packet = null;
                }

                try{
                    Thread.sleep(1000/CAM_RATE);
                }catch(InterruptedException e){
                    System.out.println("[CAM] Cam service interrupted during sleep.");
                }
                
                try{
                    send.send(packet);
                }catch(IOException e){
                    System.out.println("[ERROR] Failed to send CAM: " + e);
                }

                //Enumerate message
                generationDeltaTime = (generationDeltaTime+1);
                if(generationDeltaTime > 65536) generationDeltaTime = 0;

                //Emulate several vehicles
                station_id_offset++;
                if(station_id_offset > NUM_VEHICLES-1) station_id_offset = 0;
            }
            System.out.println("[CAM] Stopping service!");
        }
    }

    private static class DenmService implements Runnable{
        public void run(){
            System.out.println("[DENM] Starting service at " + DENM_RATE + "Hz!");
            ByteBuffer denmBuffer = ByteBuffer.wrap(denmData);            
            int station_id_offset = 0;
            while(camData != null){
                denmBuffer.putInt(1,STATION_ID+station_id_offset);
                DatagramPacket packet;
                denmData = denmBuffer.array();                
                try{
                    packet =
                        new DatagramPacket(denmData, denmData.length,
                                           InetAddress.getByName(VEHICLE_ADAPTER_IP),
                                           VEHICLE_ADAPTER_UDP_PORT);
                }catch(UnknownHostException e){
                    packet = null;
                }

                try{
                    Thread.sleep(1000/DENM_RATE);
                }catch(InterruptedException e){
                    System.out.println("[DENM] Service interrupted during sleep.");
                }
                
                try{
                    send.send(packet);
                }catch(IOException e){
                    System.out.println("[ERROR] Failed to send DENM!");
                }

                //Emulate several vehicles
                station_id_offset++;
                if(station_id_offset > NUM_VEHICLES-1) station_id_offset = 0;                
            }
            System.out.println("[DENM] Stopping service!");
        }
    }


    private static class IclcmService implements Runnable{
        public void run(){
            System.out.println("[iCLCM] Starting service at " + ICLCM_RATE + "Hz!");
            ByteBuffer iclcmBuffer = ByteBuffer.wrap(iclcmData);
            int station_id_offset = 0;
            while(camData != null){
                iclcmBuffer.putInt(1,STATION_ID+station_id_offset);                
                DatagramPacket packet;
                iclcmData = iclcmBuffer.array();
                try{
                    packet =
                        new DatagramPacket(iclcmData, iclcmData.length,
                                           InetAddress.getByName(VEHICLE_ADAPTER_IP),
                                           VEHICLE_ADAPTER_UDP_PORT);
                }catch(UnknownHostException e){
                    packet = null;
                }

                try{
                    Thread.sleep(1000/ICLCM_RATE);
                }catch(InterruptedException e){
                    System.out.println("[iCLCM] Service interrupted during sleep.");
                }
                
                try{
                    send.send(packet);
                }catch(IOException e){
                    System.out.println("[ERROR] Failed to send iCLCM!");
                }

                //Emulate several vehicles
                station_id_offset++;
                if(station_id_offset > NUM_VEHICLES-1) station_id_offset = 0;                
            }
            System.out.println("[iCLCM] Stopping service!");
        }

    }
    
    static long numMessages;
    static long startTime;
    static Hashtable<Integer, Long> denmTable= new Hashtable<Integer, Long>();
    static Hashtable<Integer, Long> camTable= new Hashtable<Integer, Long>();
    static Hashtable<Integer, Long> iclcmTable= new Hashtable<Integer, Long>();    
    public static DatagramPacket receive(){
        int stationId;
        long messageRate;
        ByteBuffer packetData;        
        DatagramPacket packet =
            new DatagramPacket(new byte[MAX_UDP_SIZE], MAX_UDP_SIZE);

        try{
            receive.receive(packet);
        }catch(IOException e){
            System.out.println("ERROR: Failed to receive packet!");
        }

        packetData = ByteBuffer.wrap(packet.getData());
        stationId = packetData.getInt(1);
        if(!denmTable.containsKey(stationId)) denmTable.put(stationId, (long) 0);
        if(!camTable.containsKey(stationId)) camTable.put(stationId, (long) 0);
        if(!iclcmTable.containsKey(stationId)) iclcmTable.put(stationId, (long) 0);
        
        switch(packet.getData()[0]){
        case 1:
            //Get packet data
            denmRecData = packet.getData();

            //Count number of messages from that station
            numMessages = denmTable.get(stationId);
            numMessages++;
            denmTable.put(stationId, numMessages);

            //Calculate average message rate
            messageRate = numMessages * 1000000 /
                (System.currentTimeMillis() - startTime);
                    
            if(verbosity > 1)
                System.out.println("[Received] DENM  \t ID: " + stationId
                                   + "\t #messages: " + numMessages
                                   + "\t Rate: " + messageRate
                                   + "\t Data: " + packet.getData());
            break;
        case 2:
            camRecData = packet.getData();
                    
            numMessages = camTable.get(stationId);
            numMessages++;
            camTable.put(stationId, numMessages);

            messageRate = numMessages * 1000000 /
                (System.currentTimeMillis() - startTime);
                    
            if(verbosity > 1)
                System.out.println("[Received] CAM  \t ID: " + stationId
                                   + "\t #messages: " + numMessages
                                   + "\t Rate: " + messageRate
                                   + "\t Data: " + packet.getData());

            if(verbosity > 2)
                System.out.println(new CAM(packet.getData()).toString());
            break;
        case 10:
            iclcmRecData = packet.getData();
                    
            numMessages = iclcmTable.get(stationId);
            numMessages++;
            iclcmTable.put(stationId, numMessages);

            messageRate = numMessages * 1000000 /
                (System.currentTimeMillis() - startTime);
                    
            if(verbosity > 1)                    
                System.out.println("[Received] iCLCM\t ID: " + stationId
                                   + "\t #messages: " + numMessages
                                   + "\t Rate: " + messageRate
                                   + "\t Data: " + packet.getData());                    
            break;
        default:
            System.out.println("WARN: Received packet with unknown message ID " + packet.getData()[0]);
        }
        return packet;
    }

    public static CAM receiveCam(){
        return new CAM(receive().getData());
    }

    
    private static class ReceiveService implements Runnable{        
        public void run(){
            System.out.println("[RECEIVE] Starting service on port " + RECEIVE_PORT + "!");
            startTime = System.currentTimeMillis();
            
            while(true){
                receive();
            }
        }
    }

    private static class DelayTestService implements Runnable{
        public void run(){
            System.out.println("[DelayTest] Starting...");
            
            long startTime;
            long elapsedTime;
            byte refValue;
            long totalTime;
            int movAvgLen = CAM_RATE;
            camData[1] = 1;

            while(true){
                totalTime = 0;
                for(int i = 0;i < movAvgLen;i++){
                    refValue = (byte) ((camData[1] + 1) % 2); //Toggle value
                    startTime = System.nanoTime();
                    camData[1] = refValue;
                    //while(camRecData[1] != refValue);

                    try{
                        while(camRecData[1] != refValue) Thread.sleep(0, 1);
                    }catch(InterruptedException e){
                        System.out.println("[WARN] DTS interrupted.");
                    }

                    elapsedTime = System.nanoTime() - startTime;
                    totalTime += elapsedTime;
                }
                System.out.println("[DelayTest] CAM delay = " + totalTime/movAvgLen);
            }
        }
    }

    private static class PacketLossTestService implements Runnable{
        public void run(){
            System.out.println("[PacketLossTest] Starting...");
            byte refValue;
            int lostPackets = 0;
            int totalPackets = 0;

            long sleepStart;
            
            while(true){
                totalPackets++;                
                refValue = (byte) ((camData[1] + 1) % 2); //Toggle value                
                camData[2] = refValue;

                sleepStart = System.nanoTime();
                try{                    
                    Thread.sleep(1000/CAM_RATE);
                }catch(InterruptedException e){
                    System.out.println("[WARN] PLTS interrupted.");
                }

                if((System.nanoTime() - sleepStart) >
                   (1000000000/CAM_RATE + 1000000000/1000)){
                    System.out.println("[WARN] Didn't wake in time to measure packet loss.");
                }else if(camRecData[2] != camData[2]){
                    lostPackets++;                    
                    System.out.println("[PacketLossTest]: " +
                                       lostPackets + "/" + totalPackets +
                                       " packets lost!");
                }
            }
        }
    }

    private static class CAM{
        byte messageId;
        int stationId;
        int generationDeltaTime;
        byte containerMask;
        int stationType;
        int latitude;
        int longitude;
        int semiMajorConfidence;
        int semiMinorConfidence;
        int semiMajorOrientation;
        int altitude;
        int headingValue;
        int headingConfidence;
        int speedValue;
        int speedConfidence;
        int vehicleLength;
        int vehicleWidth;
        int longitudinalAcc;
        int longitudinalAccConf;
        int yawRateValue;
        int yawRateConfidence;
        int vehicleRole;

        public CAM(byte[] buffer){
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            messageId = byteBuffer.get();
            stationId = byteBuffer.getInt();
            generationDeltaTime = byteBuffer.getInt();
            containerMask = byteBuffer.get();
            stationType = byteBuffer.getInt();
            latitude = byteBuffer.getInt();
            longitude = byteBuffer.getInt();
            semiMajorConfidence = byteBuffer.getInt();
            semiMinorConfidence = byteBuffer.getInt();
            semiMajorOrientation = byteBuffer.getInt();
            altitude = byteBuffer.getInt();
            headingValue = byteBuffer.getInt();
            headingConfidence = byteBuffer.getInt();
            speedValue = byteBuffer.getInt();
            speedConfidence = byteBuffer.getInt();
            vehicleLength = byteBuffer.getInt();
            vehicleWidth = byteBuffer.getInt();
            longitudinalAcc = byteBuffer.getInt();
            longitudinalAccConf = byteBuffer.getInt();
            yawRateValue = byteBuffer.getInt();
            yawRateConfidence = byteBuffer.getInt();
            vehicleRole = byteBuffer.getInt();
        }        

        public String toString(){
            return "messageId:\t\t" + messageId + "\n" +
                "stationId:\t\t" + stationId + "\n" +
                "generationDeltaTime:\t" + generationDeltaTime + "\n" +
                "containerMask:\t\t" + containerMask + "\n" +
                "stationType:\t\t" + stationType + "\n" +
                "latitude:\t\t" + latitude + "\n" +
                "longitude:\t\t" + longitude + "\n" +
                "semiMajorConfidence:\t" + semiMajorConfidence + "\n" +
                "semiMinorConfidence:\t" + semiMinorConfidence + "\n" +
                "semiMajorOrientation:\t" + semiMajorOrientation + "\n" +
                "altitude:\t\t" + altitude + "\n" +
                "headingValue:\t\t" + headingValue + "\n" +
                "headingConfidence:\t" + headingConfidence + "\n" +
                "speedValue:\t\t" + speedValue + "\n" +
                "speedConfidence:\t" + speedConfidence + "\n" +
                "vehicleLength:\t\t" + vehicleLength + "\n" +
                "vehicleWidth:\t\t" + vehicleWidth + "\n" +
                "longitudinalAcc:\t" + longitudinalAcc + "\n" +
                "longitudinalAccConf:\t" + longitudinalAccConf + "\n" +
                "yawRateValue:\t\t" + yawRateValue + "\n" +
                "yawRateConfidence:\t" + yawRateConfidence + "\n" +
                "vehicleRole:\t\t" + vehicleRole;
        }
    }

    static void setupCam(){
        byte[] buffer = new byte[MAX_UDP_SIZE];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        byteBuffer.put((byte) 2); //messageID
        byteBuffer.putInt(STATION_ID); //stationID
        byteBuffer.putInt(234); //generationDeltaTime
        byteBuffer.put((byte) 128); //containerMask
        byteBuffer.putInt(5); //stationType                
        byteBuffer.putInt(2); //latitude
        byteBuffer.putInt(48); //longitude
        byteBuffer.putInt(0); //semiMajorConfidence
        byteBuffer.putInt(0); //semiMinorConfidence
        byteBuffer.putInt(0); //semiMajorOrientation
        byteBuffer.putInt(400); //altitude
        byteBuffer.putInt(1); //heading value
        byteBuffer.putInt(1); //headingConfidence
        byteBuffer.putInt(0); //speedValue
        byteBuffer.putInt(1); //speedConfidence        
        byteBuffer.putInt(40); //vehicleLength
        byteBuffer.putInt(20); //vehicleWidth
        byteBuffer.putInt(234); //longitudinalAcc
        byteBuffer.putInt(1); //longitudinalAccConf
        byteBuffer.putInt(2); //yawRateValue
        byteBuffer.putInt(1); //yawRateConfidence
        byteBuffer.putInt(0); //vehicleRole

        camData = buffer;
        camRecData = new byte[buffer.length];
    }

    static void setupDenm(){
        byte[] buffer = new byte[MAX_UDP_SIZE];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        byteBuffer.put((byte) 1); //messageId
        byteBuffer.putInt(STATION_ID); //stationID
        byteBuffer.putInt(1000); //generationDeltaTime
        byteBuffer.put((byte) 160); //containerMask
        byteBuffer.put((byte) 64); //managementMask
        byteBuffer.putInt(1); //detectionTime
        byteBuffer.putInt(2); //referenceTime
        byteBuffer.putInt(0); //termination
        byteBuffer.putInt(900000001); //latitude
        byteBuffer.putInt(1800000001); //longtitude
        byteBuffer.putInt(1); //semiMajorConfidence
        byteBuffer.putInt(2); //semiMinorConfidence
        byteBuffer.putInt(2); //semiMajorOrientation
        byteBuffer.putInt(3); //altitude
        byteBuffer.putInt(0); //relevanceDistance
        byteBuffer.putInt(0); //relevanceTrafficDirection
        byteBuffer.putInt(0); //validityDuration
        byteBuffer.putInt(1); //transmissionIntervall
        byteBuffer.putInt(5); //stationType
        byteBuffer.put((byte) 128);    //situationMask
        byteBuffer.putInt(4); //informationQuality
        byteBuffer.putInt(2); //causeCode
        byteBuffer.putInt(2); //subCauseCode
        byteBuffer.putInt(0); //linkedCuaseCode
        byteBuffer.putInt(0); //linkedSubCauseCode
        byteBuffer.put((byte) 8); //alacarteMask
        byteBuffer.putInt(0); //lanePosition
        byteBuffer.putInt(0); //temperature
        byteBuffer.putInt(5); //positioningSolutionType

        denmData = buffer;
        denmRecData = new byte[buffer.length];
    }

    static void setupIclcm(){
        byte[] buffer = new byte[MAX_UDP_SIZE];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        byteBuffer.put((byte) 10); //messageID
        byteBuffer.putInt(STATION_ID); //stationID
        byteBuffer.put((byte) 128); //containerMask
        byteBuffer.putInt(100); //rearAxleLocation
        byteBuffer.putInt(0); //controllerType
        byteBuffer.putInt(1001); //responseTimeConstant
        byteBuffer.putInt(1001); //responseTimeDelay
        byteBuffer.putInt(10); //targetLongAcc
        byteBuffer.putInt(1); //timeHeadway
        byteBuffer.putInt(3); //cruiseSpeed
        byteBuffer.put((byte) 128); //lowFrequencyMask
        byteBuffer.putInt(1); //participantsReady
        byteBuffer.putInt(0); //startPlatoon
        byteBuffer.putInt(1); //endOfScenario
        byteBuffer.putInt(255); //mioID
        byteBuffer.putInt( 10); //mioRange
        byteBuffer.putInt(11); //mioBearing
        byteBuffer.putInt(12); //mioRangeRate
        byteBuffer.putInt(3); //lane
        byteBuffer.putInt(0); //forwardID
        byteBuffer.putInt(0); //backwardID
        byteBuffer.putInt(0); //mergeRequest
        byteBuffer.putInt(0); //safeToMerge
        byteBuffer.putInt(1); //flag
        byteBuffer.putInt(0); //flagTail
        byteBuffer.putInt(1); //flagHead
        byteBuffer.putInt(254); //platoonID
        byteBuffer.putInt(100); //distanceTravelledCz
        byteBuffer.putInt(2); //intention
        byteBuffer.putInt(2); //counter

        iclcmData = buffer;
        iclcmRecData = new byte[buffer.length];
    }

    public static void main(String args[]) throws IOException{
        System.out.println("VA test suite. A small application for evaluating the open source Geonetworking Vehicle Adapter. Author: Albin Severinson.\n" +
                           "Usage: 'java VaTestSuite' followed by any of these arguments:\n" +
                           "--nocam\n" +
                           "--nodenm\n" +
                           "--noiclcm\n" +
                           "--camrate <rate in Hz>\n" +
                           "--denmrate <rate in Hz>\n" +
                           "--iclcmrate <rate in Hz>\n" +
                           "--receiveport <port>\n" +
                           "--vehicleaddress <ip:port>\n" +
                           "--stationid <id>\tStation ID this application will use.\n" +
                           "--numvehicles <num>\tNumber of vehicles to emulate. StationID of the other vehicles will be set as increments of 1 from the ID defined by the --stationid argument. The messages are transmitted in series. Increase the rate of CAM/DENM/iCLCM using the respective arguments to preserve the per-vehicle message rate.\n" +
                           "\nIf an option is not present, default values will be used.\n" +
                           "Examples:\n" +
                           "Start the VA test suit with default options: 'java VaTestSuite'\n" +
                           "Without iCLCM: 'java VaTestSuite --withouticlcm'\n" +
                           "Without DENM and custom CAM rate: 'java VaTestSuite --withoutdenm --camrate 17'\n" +
                           "With a custom vehicle IP: 'java VaTestSuite --vehicleaddress 192.168.1.19:5000'\n"+
                           "Emulating several vehicles: 'java VaTestSuite --numvehicles 2 --camrate 50 --denmrate 20 --iclcmrate 50'\n");


        /* Parse command line arguments */
        boolean withCam = true;
        boolean withDenm = true;
        boolean withIclcm = true;
        for(int i = 0;i < args.length;i++){
            String s = args[i];
            if(s.equals("--nocam")) withCam = false; 
            else if(s.equals("--nodenm")) withDenm = false;
            else if(s.equals("--noiclcm")) withIclcm = false;
            else if(s.equals("--camrate")){
                try{
                    i++;
                    CAM_RATE = Integer.parseInt(args[i]);
                }catch(NumberFormatException|ArrayIndexOutOfBoundsException e){
                    System.err.println("Argument " + args[i-1] + " must be followed by an integer.");
                    System.exit(1);
                }
            }
            else if(s.equals("--denmrate")){
                try{
                    i++;
                    DENM_RATE = Integer.parseInt(args[i]);
                }catch(NumberFormatException|ArrayIndexOutOfBoundsException e){
                    System.err.println("Argument " + args[i-1] + " must be followed by an integer.");
                    System.exit(1);
                }
            }
            else if(s.equals("--iclcmrate")){
                try{
                    i++;
                    ICLCM_RATE = Integer.parseInt(args[i]);
                }catch(NumberFormatException|ArrayIndexOutOfBoundsException e){
                    System.err.println("Argument " + args[i-1] + " must be followed by an integer.");
                    System.exit(1);
                }
            }
            else if(s.equals("--receiveport")){
                try{
                    i++;
                    RECEIVE_PORT = Integer.parseInt(args[i]);
                }catch(NumberFormatException|ArrayIndexOutOfBoundsException e){
                    System.err.println("Argument " + args[i-1] + " must be followed by an integer.");
                    System.exit(1);
                }
            }
            else if(s.equals("--vehicleaddress")){
                try{
                    i++;
                    String[] address_split = args[i].split(":");
                    VEHICLE_ADAPTER_IP = address_split[0];
                    VEHICLE_ADAPTER_UDP_PORT = Integer.parseInt(address_split[1]);
                }catch(NumberFormatException|ArrayIndexOutOfBoundsException e){
                    System.err.println("Argument " + args[i-1] +
                                       " must be followed by an address on the format ip:port.");
                    System.exit(1);
                }
            }
            else if(s.equals("--stationid")){
                try{
                    i++;
                    STATION_ID = Integer.parseInt(args[i]);
                }catch(NumberFormatException|ArrayIndexOutOfBoundsException e){
                    System.err.println("Argument " + args[i-1] + " must be followed by an integer.");
                    System.exit(1);
                }
            }
            else if(s.equals("--numvehicles")){
                try{
                    i++;
                    NUM_VEHICLES = Integer.parseInt(args[i]);
                }catch(NumberFormatException|ArrayIndexOutOfBoundsException e){
                    System.err.println("Argument " + args[i-1] + " must be followed by an integer.");
                    System.exit(1);
                }
            }                
        }

        /* Set up data */
        setupCam();
        setupDenm();
        setupIclcm();
        
        /* Set up sockets */
        send = new DatagramSocket();
        receive = new DatagramSocket(RECEIVE_PORT);
        
        /* Start services */
        System.out.println("** STARTING **");
        System.out.println("[MAIN] Sending to: " +
                           VEHICLE_ADAPTER_IP + ":" +
                           VEHICLE_ADAPTER_UDP_PORT);
        if(withCam){
            Thread cs = new Thread(new CamService());
            cs.setPriority(10);
            cs.start();
        }

        if(withDenm){
            Thread ds = new Thread(new DenmService());
            ds.setPriority(10);
            ds.start();
        }

        if(withIclcm){
            Thread is = new Thread(new IclcmService());
            is.setPriority(10);
            is.start();
        }

        
        Thread rs = new Thread(new ReceiveService());
        rs.setPriority(10);
        rs.start();

        /*
        Thread dts = new Thread(new DelayTestService());
        dts.setPriority(9);
        dts.start();

        Thread plts = new Thread(new PacketLossTestService());
        plts.setPriority(9);
        plts.start();
        */

    }
}
