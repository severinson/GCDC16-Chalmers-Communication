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

    static String VEHICLE_ADAPTER_IP = "127.0.0.1";
    static int VEHICLE_ADAPTER_UDP_PORT = 5003;
    static int RECEIVE_PORT = 5000;
    static int MAX_UDP_SIZE = 2000;
    static int CAM_RATE = 25;
    static int DENM_RATE = 25;
    static int ICLCM_RATE = 25;
    static int verbosity = 2;
    
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
            System.out.println("[CAM] Starting service!");
            while(camData != null){
                DatagramPacket packet;
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
                    System.out.println("[ERROR] Failed to send CAM!");
                }
            }
            System.out.println("[CAM] Stopping service!");
        }
    }

    private static class DenmService implements Runnable{
        public void run(){
            System.out.println("[DENM] Starting service!");
            while(camData != null){
                DatagramPacket packet;
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
            }
            System.out.println("[DENM] Stopping service!");
        }
    }


    private static class IclcmService implements Runnable{
        public void run(){
            System.out.println("[iCLCM] Starting service!");
            while(camData != null){
                DatagramPacket packet;
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
            }
            System.out.println("[iCLCM] Stopping service!");
        }
    }

    
    private static class ReceiveService implements Runnable{
        public void run(){
            System.out.println("[RECEIVE] Starting service!");
            ByteBuffer packetData;
            int stationId;
            int numMessages;
            long messageRate;
            long startTime = System.currentTimeMillis();

            Hashtable<Integer, Integer> denmTable= new Hashtable<Integer, Integer>();
            Hashtable<Integer, Integer> camTable= new Hashtable<Integer, Integer>();
            Hashtable<Integer, Integer> iclcmTable= new Hashtable<Integer, Integer>();
            
            while(true){
                DatagramPacket packet =
                    new DatagramPacket(new byte[MAX_UDP_SIZE], MAX_UDP_SIZE);

                try{
                    receive.receive(packet);
                }catch(IOException e){
                    System.out.println("ERROR: Failed to received packet!");
                }

                packetData = ByteBuffer.wrap(packet.getData());
                stationId = packetData.getInt(1);
                if(!denmTable.containsKey(stationId)) denmTable.put(stationId, 0);
                if(!camTable.containsKey(stationId)) camTable.put(stationId, 0);
                if(!iclcmTable.containsKey(stationId)) iclcmTable.put(stationId, 0);
                
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
                    System.out.println("WARN: Received packet with unknown message ID.");
                }
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

    static void setupCam(){
        byte[] buffer = new byte[MAX_UDP_SIZE];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        byteBuffer.put((byte) 2); //messageID
        byteBuffer.putInt(1337); //stationID
        byteBuffer.putInt(1); //generationDeltaTime
        byteBuffer.put((byte) 128); //containerMask
        byteBuffer.putInt(5); //stationType                
        byteBuffer.putInt(900000001); //latitude
        byteBuffer.putInt(1800000001); //longitude
        byteBuffer.putInt(0); //semiMajorConfidence
        byteBuffer.putInt(0); //semiMinorConfidence
        byteBuffer.putInt(0); //semiMajorOrientation
        byteBuffer.putInt(0); //altitude
        byteBuffer.putInt(1); //heading value
        byteBuffer.putInt(1); //headingConfidence
        byteBuffer.putInt(0); //speedValue
        byteBuffer.putInt(1); //speedConfidence        
        byteBuffer.putInt(40); //vehicleLength
        byteBuffer.putInt(20); //vehicleWidth
        byteBuffer.putInt(0); //longitudinalAcc
        byteBuffer.putInt(1); //longitudinalAccConf
        byteBuffer.putInt(2); //yawRateValue
        byteBuffer.putInt(1); //yawRateConfidence        
        byteBuffer.putInt(5); //vehicleRole

        camData = buffer;
        camRecData = new byte[buffer.length];
    }

    static void setupDenm(){
        byte[] buffer = new byte[MAX_UDP_SIZE];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        byteBuffer.put((byte) 1); //messageId
        byteBuffer.putInt(1337); //stationID
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
        byteBuffer.putInt(0); //transmissionIntervall
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
        byteBuffer.putInt(1337); //stationID
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
        byteBuffer.putInt(0); //endOfScenario
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
        /* Set up sockets */
        send = new DatagramSocket();
        receive = new DatagramSocket(RECEIVE_PORT);

        /* Set up data */
        setupCam();
        setupDenm();
        setupIclcm();

        /* Start services */
        Thread cs = new Thread(new CamService());
        cs.setPriority(10);
        cs.start();
        
        Thread ds = new Thread(new DenmService());
        ds.setPriority(10);
        ds.start();

        Thread is = new Thread(new IclcmService());
        is.setPriority(10);
        is.start();        
        
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
