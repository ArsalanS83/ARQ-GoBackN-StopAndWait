package transport;

public class Receiver extends NetworkHost {
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
    
    // Add any necessary class variables here. They can hold state information for the receiver.
    // Also add any necessary methods (e.g. checksum of a String)
    
    private int expectedSeqNum;
    private Packet lastSentACK;
    
    // This is the constructor.  Don't touch!
    public Receiver(int entityName) {
        super(entityName);
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
           
           // if data is empty set sum to 100 and add it seqNo and the ack num
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
     * This method will be called once, before any of your other receiver-side methods are called
     * It can be used to do any required initialization
     * (e.g. of member variables you add to control the state of the receiver)
     */
       @Override
       public void init() {
           // initially expecting sequence number of 0 (first packet)
           expectedSeqNum = 0;
       }

    // This method will be called whenever a packet sent from the sender(i.e. as a result of a udtSend() being called by the Sender ) arrives at the receiver. 
    // The argument "packet" is the (possibly corrupted) packet sent from the sender.
       @Override
       public void input(Packet packet) {
           
           int checksum = computeChecksum(packet.getSeqnum(),packet.getAcknum(),packet.getPayload());
           
           // If packet is not corrupted and matches expected sequence number
           // ensures always sending ACK for recieved packet with highest in order sequence number
           if (packet.getChecksum() == checksum && packet.getSeqnum() == expectedSeqNum)
           {
               // deliver the data to the application layer
               String data = packet.getPayload();
               deliverData(data);
               
               // send the appropriate acknowledgement
               int ackChecksum = computeChecksum(0,expectedSeqNum,"");
               Packet ack = new Packet(0,expectedSeqNum,ackChecksum);
               udtSend(ack);
               
               // store last sent acknowledgement
               // used to later resend acknowledgement with highest in order sequence number
               lastSentACK = ack;
               
               // increase expected sequence number for next expected packet
               expectedSeqNum++;
           }
           else
           {
               // if packet recieved is out of order or packet is corrupted
               // and there is history of a previously sent ACK
               // resend last ACK with highest sequence number
               if ((checksum != packet.getChecksum()) || packet.getSeqnum() != expectedSeqNum
                       && lastSentACK != null)
               {
                   Packet ack = new Packet(0,lastSentACK.getAcknum(),lastSentACK.getChecksum());
                   udtSend(ack);
               }
           }
       }
}