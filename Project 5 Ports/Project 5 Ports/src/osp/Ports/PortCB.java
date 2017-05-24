/**
 * @author Jeffrey Cocklin
 * @class CSCE 311 section 1
 * Project 5 Ports
 */

package osp.Ports;

import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.Utilities.*;

/**
   The studends module for dealing with ports. The methods 
   that have to be implemented are do_create(), 
   do_destroy(), do_send(Message msg), do_receive(). 

   
   @OSPProject Ports
*/



public class PortCB extends IflPortCB
{
    /**
       Creates a new port. This constructor must have

	   super();

       as its first statement.

       @OSPProject Ports
    */
    
	private int avlBufferSpace;
	
	
	public PortCB()
    {
        // your code goes here
    	super();
    	this.avlBufferSpace= PortBufferLength;
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Ports
    */
    public static void init()
    {
        // your code goes here
    	
    }

    /** 
        Sets the properties of a new port, passed as an argument. 
        Creates new message buffer, sets up the owner and adds the port to 
        the task's port list. The owner is not allowed to have more 
        than the maximum number of ports, MaxPortsPerTask.

        @OSPProject Ports
    */
    public static PortCB do_create()
    {
        // your code goes here
    	
    	// currentTask=null;
    	
    	try{
    		TaskCB currentTask= MMU.getPTBR().getTask();   //get current task
    		    		
    		if(currentTask.getPortCount() <= MaxPortsPerTask){// if maxports not exceeded,
    			
    			PortCB newPort = new PortCB(); // create the new port.
    			
    			if(currentTask.addPort(newPort) != FAILURE){// try to add the port
    	     	
    	     	    newPort.setTask(currentTask);
    	     		newPort.setStatus(PortLive);   //associate port with task.
    	     	        	     		  	
    	     		return newPort;
    			}
    		} 
    	
    	}
    	catch(NullPointerException e){
    		
    	}
    	
    	return null;
    }

    /** Destroys the specified port, and unblocks all threads suspended 
        on this port. Delete all messages. Removes the port from 
        the owners port list.
        @OSPProject Ports
    */
    public void do_destroy()
    {
        // your code goes here
    	this.setStatus(PortDestroyed);  //set status of Port to destroy
    	this.notifyThreads();         //alert threads to port destruction // probably suspended on data structe I made.
    	
    	this.getTask().removePort(this);  //remove port from task
    	this.setTask(null);            //reset the owner of port to null
    }

    /**
       Sends the message to the specified port. If the message doesn't fit,
       keep suspending the current thread until the message fits, or the
       port is killed. If the message fits, add it to the buffer. If 
       receiving threads are blocked on this port, resume them all.

       @param msg the message to send.

       @OSPProject Ports
    */
    public int do_send(Message msg)
    {
        // your code goes here
    	if(msg != null && msg.getLength() <= PortBufferLength){
    	    	
    		SystemEvent MyEvent= new SystemEvent("MyEvent");
    		
    		try{
    			    
    				boolean noSpace,livePort, liveThread;
    			    
    				ThreadCB currentThread= MMU.getPTBR().getTask().getCurrentThread();
    				currentThread.suspend(MyEvent);//move currentThread to System thread by suspending on new event
    				    				
    				while(true){
    					
    					if( msg.getLength() > this.avlBufferSpace){ //suspend the thread on the port if no room
    						    						
    						currentThread.suspend(this);
    						noSpace=true;
    					}
    					else{
    						noSpace=false;
    					}
    					if(currentThread.getStatus() != ThreadKill){
    						liveThread=true;
    					}
    					else{
    						liveThread=false;
    					}
    					if(this.getStatus() == PortLive){
    						livePort=true;
    					}
    					else{
    						livePort=false;
    					}
    					
    					////Loop break conditions
    					if(liveThread == false){
    						this.removeThread(currentThread);
    						break;
    					}
    					else if(livePort== false){
    						MyEvent.notifyThreads();
    						break;
    					}
    					else if(noSpace== false){
    						break;
    					}
    			}
    			
    			if(msg.getLength() <= this.avlBufferSpace && livePort==true && liveThread==true){
    				boolean prevEmpty=false;
    				
    				if(this.isEmpty())
    					prevEmpty=true;
    				
//    				MyOut.print(this, "msgLength="+msg.getLength()+"HEY!" );
//    				MyOut.print(this, "avlBufferSpance="+this.avlBufferSpace+"HEY!" );    			
    				
    				this.appendMessage(msg);
    				
    				if(prevEmpty==true)
						this.notifyThreads();
    				
    				this.avlBufferSpace= this.avlBufferSpace - msg.getLength();
    				
//    				MyOut.print(this, "aftersubtrtmsgLength="+msg.getLength()+"HEY!" );
//    				MyOut.print(this, "avlBufferSpance="+this.avlBufferSpace+"HEY!" );
    				
    				if(this.avlBufferSpace < 0)
    					this.avlBufferSpace=0;
    				
//    				MyOut.print(this, "after<0msgLength="+msg.getLength()+"HEY!" );
//    				MyOut.print(this, "avlBufferSpance="+this.avlBufferSpace+"HEY!" );
    				
    				MyEvent.notifyThreads();
    				
    				return SUCCESS;
    			}
    		}
    	
    		    		
    		catch(NullPointerException e){}
    	}
    	return FAILURE;
    }

    /** Receive a message from the port. Only the owner is allowed to do this.
        If there is no message in the buffer, keep suspending the current 
	thread until there is a message, or the port is killed. If there
	is a message in the buffer, remove it from the buffer. If 
	sending threads are blocked on this port, resume them all.
	Returning null means FAILURE.

        @OSPProject Ports
    */
    public Message do_receive() 
    {
        // your code goes here
    	try{
    		ThreadCB currentThread= MMU.getPTBR().getTask().getCurrentThread();
    		
    		if(this.getTask() ==currentThread.getTask()){
    			
    			SystemEvent MyEventR= new SystemEvent("MyEventR");
    			currentThread.suspend(MyEventR);
    			
    			boolean empty, livePort, liveThread;
    			
    			while(true){
					
					if( this.isEmpty()){ //suspend the thread on the port if no messages are available
						currentThread.suspend(this);
						empty=true;
					}
					else{
						empty=false;
					}
					if(currentThread.getStatus() != ThreadKill){
						liveThread=true;
					}
					else{
						liveThread=false;
					}
					if(this.getStatus() == PortLive){
						livePort=true;
					}
					else{
						livePort=false;
					}
					
					////Loop break; conditions
					if(liveThread == false){ // thread status is ThreadKill
						this.removeThread(currentThread);
						break;
					}
					else if(livePort== false){ // Port is destroyed
						MyEventR.notifyThreads();
						break;
					}
					else if(empty== false){ //We have a message!
						break;
					}
			}
			
			if(empty == false && livePort==true && liveThread==true){
				
								
				Message newMsg= this.removeMessage(); // get the message out of the port
				
//				MyOut.print(this, "RecievemsgLength="+newMsg.getLength()+"HEY!" );
//				MyOut.print(this, "avlBufferSpance="+this.avlBufferSpace+"HEY!" );
				
				this.avlBufferSpace= this.avlBufferSpace + newMsg.getLength(); //update amount of space in buffer
				
//				MyOut.print(this, "after+RecievemsgLength="+newMsg.getLength()+"HEY!" );
//				MyOut.print(this, "avlBufferSpance="+this.avlBufferSpace+"HEY!" );
				
				if(this.avlBufferSpace > PortBufferLength)
					this.avlBufferSpace= PortBufferLength;
				
//				MyOut.print(this, "after >PBLRecievemsgLength="+newMsg.getLength()+"HEY!" );
//				MyOut.print(this, "avlBufferSpance="+this.avlBufferSpace+"HEY!" );
				
				this.notifyThreads();  
				MyEventR.notifyThreads();
								
				return newMsg;
			}
    			
    	}
         		
   	}catch(NullPointerException e){}
    	
    	return null;
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Ports
    */
    public static void atError()
    {
         //your code goes here
    	
  
    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
	@OSPProject Ports
    */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
