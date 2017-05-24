/**
 * @author Jeffrey Cocklin
 * @email jcocklin@email.sc.edu
 * CSCE 311 section 1
 */


package osp.Threads;
import java.util.Vector;
import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
    
	static GenericList readyThreads;
	
	/**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    public ThreadCB()
    {
        // your code goes here
        super(); // call parent constructor.
    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
        // your code goes here
        readyThreads = new  GenericList();  //initiate the "ready queue"
       
    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
        
    	// your code goes here
        if(task == null || task.getThreadCount() >= MaxThreadsPerTask  ){ // if task is null or has reached max threads dispatch thread 
        	dispatch();
        	return null;                                                  // failed to create thread
        }
        
        ThreadCB newThread= new ThreadCB();       //create a new thread
        newThread.setPriority(task.getPriority()); // set new thread's priority, given by it's task.
        newThread.setStatus(ThreadReady);         // set new thread's status to Ready. 
        
        newThread.setTask(task);                 // associate the thread with it's task.
        
        
        int addedVal= task.addThread(newThread);  // attempt to associate the task with the new thread.
        
        if(addedVal != FAILURE){           // successfully associated task with thread
        	readyThreads.append(newThread); // new Thread in readyThreads queue.
            dispatch();                     // dispatch a new thread.
            
           // MyOut.print(newThread, "what is thread status threadready?"+ThreadReady);
            
            return newThread;
        }
        else{
        	dispatch();    //failed to associated thread with task
        	return null;   // thread creation failed.
       
        }
        	
        	
    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    { 
        // your code goes here
        int status= this.getStatus();
    	TaskCB taskToEnd= this.getTask();
    	//T//hreadCB currentThread =null;
    	
    	
        
    	if(status == ThreadReady && readyThreads.contains(this)){
        	readyThreads.remove(this); // "ThreadReady", threads to kill removed from readyThreads
        }
    	
    	
        if(status == ThreadRunning){
        	pre_empt();                  //Pre-empt Running threads to kill 
        	
        }
        
        //nothing particular for status == ThreadWaiting
   
 	
        
        this.setStatus(ThreadKill);
        
        this.getTask().removeThread(this);  //Removal of thread from task
    		
        
    	int numberOfDevices= Device.getTableSize();
        int i=0;
        
        while(numberOfDevices > 0){           //Cancel all I/O request the threads made to any devices.
        	Device.get(i).cancelPendingIO(this);
        	numberOfDevices--;
        	i++;
        }
       
        ResourceCB.giveupResources(this); // Thread to Kill must relinquish resources.
        dispatch();                       
        
        if(taskToEnd.getThreadCount() <= 0){ //Kill the task if it has no more threads.
        	taskToEnd.kill();
        	
        }
        
     
    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {    
        	
    	     //MyOut.print(currentThread,"that's the current thread.\n");
    		if(this != null){
	            
            	if(this.getStatus() >= ThreadWaiting){ //suspending a thread for longer time
            		this.setStatus(this.getStatus() + 1 );
            	}
            	else if(this.getStatus() == ThreadRunning){// running thread set to waiting and its task's current thread 
            		this.setStatus(ThreadWaiting);         //must be set to null
            	    this.getTask().setCurrentThread(null);
            	}
            	
            	if(readyThreads.contains(this)){// removing current thread from readyThreads queue
                	readyThreads.remove(this);
                }
                event.addThread(this);// the event that caused the suspend adds this thread to its waiting queue.
                dispatch();   	    	
    		} 
    	           
        
    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
        //
    	// your code goes here
       if(this != null){
	    	if(this.getStatus() < ThreadWaiting){
	    		return; //thread below waiting indicates some other status.
	    	}
	    	else{
	    		if(this.getStatus() > ThreadWaiting){
	    			this.setStatus(this.getStatus()- 1); // if Thread already at some waiting level 1 or above, decrement waiting level.
	    		    
	    		}
	    		else if(this.getStatus() == ThreadWaiting){
	    			this.setStatus(ThreadReady); // resuming at threadWaiting level 0 , so lead to ThreadReady status.
	    			
	    		}
	    	    if (this.getStatus() == ThreadReady){
	    	    	readyThreads.append(this); // Put a ready Thread back into the readyThreads queue.
	    	    }
	    	}
	    	dispatch();
       }
    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    public static int do_dispatch()
    {
      
    	ThreadCB currentThread= pre_empt(); // initiate thread preempt.
      
    	if(currentThread != null)
    		readyThreads.append(currentThread);
       
    	if(!readyThreads.isEmpty()){
    	  
    		ThreadCB nextThread= (ThreadCB)readyThreads.removeHead(); // get the next thread from readyThreads queue.
    		MMU.setPTBR(nextThread.getTask().getPageTable()); //Access the MMU and set pointer to the Pagetable belonging to nextThread.
           
    		nextThread.setStatus(ThreadRunning); //Change nextThread status.
    		nextThread.getTask().setCurrentThread(nextThread); // Set nextThread's task, so nextThread is the current thread.
       	       		
    		return SUCCESS;
        }
            	
    	return FAILURE;
   }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
     */
    public static void atWarning()
    {
        // your code goes here

    }

    private static ThreadCB pre_empt(){
    	ThreadCB currentThread =null;
    	TaskCB currentTask= null;
    	
    	try{
    	
    		currentTask= MMU.getPTBR().getTask(); // Try to get the current task from MMU.
    		currentThread= currentTask.getCurrentThread();// Try to get the current Thread.
    	
    		if(currentThread != null){
    			currentThread.setStatus(ThreadReady); //Successfully encountered current Thread, so Change it's status
        		currentTask.setCurrentThread(null); // Change the Thread's task so, it's current Thread is null    	
        	}
    		
    	}catch( NullPointerException e){
    		// Null value from getPTBR() encountered so do nothing.
    		
    	}
    	    	    	    	
    	MMU.setPTBR(null); // Access MMU and set PTBR to null
    	    	
    	return currentThread; 
    }
    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
