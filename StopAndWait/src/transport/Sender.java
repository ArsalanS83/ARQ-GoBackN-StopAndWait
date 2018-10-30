package transport;

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
    
    private int sequenceNo; // sequenceNo for packet sent from application layer
    private int expectedAckNo; // expected ack number for packet from reciever
    private Packet lastSentPacket; // last packet sent to reciever
    
    
    // This is the constructor.  Don't touch!
    public Sender(int entityName) {
        super(entityName);
    }


    /**
     * This method will be called once, before any of your other sender-side methods are called. 
     * It can be used to do any required initialization (e.g. of member variables you add to control
     */
       @Override
       public void init() {
           // initially first packet has seqNo and expectedackNo of 0
           sequenceNo = 0; 
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
     * 
     * This method will be called whenever the app layer at the sender has a message to send.
     * The job of your protocol is to ensure that the data in such a message is delivered
     * in-order, and correctly, to the receiving application layer.
     * @param message application layer message to be sent
     */
       @Override
       public void output(Message message) {
           
           // create checksum of packet using sequence number and app data
           // Sender packets have an acknowledgement of 0
           int checksum = computeChecksum(sequenceNo,0,message.getData());
           
           // create packet to send with seqNo, checksum and data from app layer
           Packet p = new Packet(sequenceNo,0,checksum,message.getData());
           udtSend(p);
           startTimer(40);
           
           // store the packet just sent, later used for retransmission if needed
           lastSentPacket = new Packet(p); 
           
           
           // if the sequenceNo of the packet sender just sent is 0 
           // flip the expected sequenceNo to allow next packet to be sent
           // and make sure sender is expecting correct ack number
           if (sequenceNo == 0)
           {
              sequenceNo = 1;
              expectedAckNo = 0;
           }
           else
               if (sequenceNo == 1)
               {
                   sequenceNo = 0;
                   expectedAckNo = 1;
               }
       }
    
    
    /**
     * This method will be called whenever a packet sent from the receiver
     * (i.e. as a result of a udtSend() being done by a receiver procedure) arrives at the sender.
     * "packet" is the (possibly corrupted) packet sent from the receiver.
     * @param packet the packet that was sent from the receiver
     */
       @Override
       public void input(Packet packet) {
           
           // Compute the checksum of the received packet
           int checksum = computeChecksum(packet.getSeqnum(),packet.getAcknum(),packet.getPayload());
           
           // computed checksum = recieved packet's checksum? 
           // is acknowledgement number expected?
           if (checksum == packet.getChecksum() && expectedAckNo == packet.getAcknum())
           {
              stopTimer();
           }
           
           // otherwise time out
}     
    
    
    /**
     * This method will be called when the senders timer expires
     * (thus generating a timer interrupt).
     * You'll probably want to use this method to control the retransmission of packets.
     * See startTimer() and stopTimer(), above, for how the timer is started and stopped.
     */
       @Override
       public void timerInterrupt() {
           udtSend(lastSentPacket); // retransmit the recently sent packet
           startTimer(40);
       }
}
