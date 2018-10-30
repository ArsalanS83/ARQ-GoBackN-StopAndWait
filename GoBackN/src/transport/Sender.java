package transport;

import java.util.ArrayList;

public class Sender extends NetworkHost {

    /*
     * Predefined Constant (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and Packet payload
     *
     *
     * Predefined Member Methods:
     *
     *  void startTimer(double increment):
     *       Starts a timer, which will expire in "increment" time units, causing the interrupt handler to be called.  You should only call this in the Sender class.
     *  void stopTimer():
     *       Stops the timer. You should only call this in the Sender class.
     *  void udtSend(Packet p)
     *       Sends the packet "p" into the network to arrive at other host
     *  void deliverData(String dataSent)
     *       Passes "dataSent" up to app layer. You should only call this in the Receiver class.
     *
     *  Predefined Classes:
     *
     *  NetworkSimulator: Implements the core functionality of the simulator
     *
     *  double getTime()
     *       Returns the current time in the simulator. Might be useful for debugging. Call it as follows: NetworkSimulator.getInstance().getTime()
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for debugging. Call it as follows: NetworkSimulator.getInstance().printEventList()
     *
     *  Message: Used to encapsulate a message coming from the application layer
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      void setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *      String getData():
     *          returns the data contained in the message
     *
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet, which is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload):
     *          creates a new Packet with a sequence field of "seq", an ack field of "ack", a checksum field of "check", and a payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an ack field of "ack", a checksum field of "check", and an empty payload
     *    Methods:
     *      void setSeqnum(int seqnum)
     *          sets the Packet's sequence field to seqnum
     *      void setAcknum(int acknum)
     *          sets the Packet's ack field to acknum
     *      void setChecksum(int checksum)
     *          sets the Packet's checksum to checksum
     *      void setPayload(String payload) 
     *          sets the Packet's payload to payload
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      String getPayload()
     *          returns the Packet's payload
     *
     */
    
    // Add any necessary class variables here. They can hold state information for the sender. 
    // Also add any necessary methods (e.g. checksum of a String)
    
    private ArrayList<Packet> buffer;
    private int endofWindow; // handles sliding from the end of the window
    private int windowSize;
    private int base;
    private int nextSeqNum;
    
    
    // This is the constructor.  Don't touch!
    public Sender(int entityName) {
        super(entityName);
    }

 
    /**
     * This method will be called once, before any of your other sender-side methods are called.
     * It can be used to do any required initialization
     * (e.g. of member variables you add to control the state of the sender).
     */
       @Override
       public void init() {
           
           // Intially nextSeqNum and base are equal
           // end of the window is currently the window size
           buffer = new ArrayList<>();
           base = 0; // represents front of window
           nextSeqNum = 0;
           endofWindow = 8; // represents end of window
           windowSize = 8; 
       }
       
     /**
     * Compute Checksum
     * 1) Convert packet data into bytes
     * 2) Sum the byte representation of the data character by character
     * 3) Add sum to sequence number and acknowledgment number of the packet
     * @param seqNo sequence number of the packet
     * @param ack acknowledgment number of the packet
     * @param data the data in the packet
     * @return checksum for the packet
     */
       public int computeChecksum(int seqNo, int ack, String data) {
           
           int sum = 0;
           
           // if data is empty set sum to 100 and add to seqNo and the ack num
           if (data.isEmpty())
           {
               sum = 100;
               return sum+seqNo+ack;
           }
           
           byte[] message = data.getBytes();
           
           for(int i = 0; i<message.length; i++){
               sum += message[i];
           }
                       
           return sum+seqNo+ack;
       }
    

    /**
     * Sends a Packet containing Application Layer Data to the Receiver
     * This method will be called whenever the application layer at the sender has a message to send
     * The job of your protocol is to ensure that the data in such a message
     * is delivered in-order, and correctly, to the receiving application layer
     * @param message data to be formulated into a packet
     */
           @Override
       public void output(Message message) {
           
           // is nextSeqNum inside usable window?
           if (nextSeqNum < base + windowSize)
           {
               // compute checksum
               int checksum = computeChecksum(nextSeqNum,0,message.getData());
               Packet p = new Packet(nextSeqNum,0,checksum,message.getData());
               // add sent packet to position at nextSeqNum
               buffer.add(nextSeqNum, p);
               udtSend(p);
               
               // if 1st packet to be sent?
               // start timer for oldest unacknowledged packet
               if (base == nextSeqNum)
               {
                   startTimer(40);
               }
               
               // increase nextSeqNum ready for next packet to be sent
               nextSeqNum++;
           }
           
           // if nextSeqNum outside usable window
           // refuse data from application layer (block app layer)
       }
    
    

    /**
     * Receives ACK Packets from Receiver
     * This method will be called whenever a packet sent from the receiver
     * as a result of a udtSend() being done by a receiver procedure) arrives at the sender
     * "packet" is the (possibly corrupted) packet sent from the receiver
     * @param packet ACK packet received
     */
       @Override
       public void input(Packet packet) {
           
           // compute checksum
           int checksum = computeChecksum(packet.getSeqnum(),packet.getAcknum(),packet.getPayload());
           
           // if packet recieved is not corrupted
           if (packet.getChecksum() == checksum)
           {
               //store acknowledgement at base location
               buffer.add(base, packet);

               // increase base when acknowledgement is recieved
               // (slides front of window)
               // also handles cumulative acknowledgements
               base = packet.getAcknum()+1;
               
               // Increase end of window to complete sliding action
               endofWindow++;
           
               // if all packets sent are acknowledged
               // stop timer because there are no more unacknowledged packets
               // if not, restart timer for the next packet waiting to be sent
               // every time window is slided timer is restarted
               if (base == nextSeqNum)
               {
                   stopTimer();
               }
               else
               {
                   startTimer(40);
               }
           } 
       }
    
    /**
     * Handles Retransmission of Sent & Unacknowledged Packets
     * This method will be called when the senders timer expires (thus generating a timer interrupt)
     * You'll probably want to use this method to control the retransmission of packets
     * See startTimer() and stopTimer(), above, for how the timer is started and stopped
     */
       @Override
       public void timerInterrupt() {
           
           // restart timer
           startTimer(40);
           
           // retransmit all sent but not yet acknowledged packets
           // start from oldest unacknowledged packet (base)
           // up to packet at position next sequence number
           
           // iterate through each packet and retransmit
           for (int i = base; i<=nextSeqNum; i++)
           {
               udtSend(buffer.get(i));
               i++;
           }
       }
}