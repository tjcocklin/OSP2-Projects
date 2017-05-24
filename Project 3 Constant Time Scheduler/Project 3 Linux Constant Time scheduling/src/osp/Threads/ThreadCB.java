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
    
	//static GenericList readyThreads;
	private static final int MY_SIZE= 10;
	private static final int LOWEST_PRIORITY= 9;
	
	private static final int HIGHEST_PRIORITY= 0;
	private static final int DEFAULT_PRIORITY= 4;
	
	static GenericList [] active_array;
	static GenericList [] expired_array;
	
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
       // readyThreads = new  GenericList();  //initiate the "ready queue"
        
        active_array = new GenericList[MY_SIZE];  // initiate two arrays
        expired_array= new GenericList[MY_SIZE];
        
        for(int i=0; i < MY_SIZE; i++){
        	active_array[i]= new GenericList(); // fill arrays with empty lists.
        	expired_array[i]= new GenericList();
        }
       
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
                        
        newThread.setPriority(DEFAULT_PRIORITY);                 // new thread priority set to 4 as default.
        newThread.setStatus(ThreadReady);         // set new thread's status to Ready. 
        
        newThread.setTask(task);                 // associate the thread with it's task.
        int addedVal= task.addThread(newThread);  // attempt to associate the task with the new thread.
        
        if(addedVal != FAILURE){           // successfully associated task with thread
            expired_array[DEFAULT_PRIORITY].append(newThread);//new Thread placed in expired array queue.
        	dispatch();                     // dispatch a new thread.
                       
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
    	
    	    	
        // "ThreadReady", threads to kill removed from active and expired arrays.
    	if(status == ThreadReady){
    		active_array[this.getPriority()].remove(this);
            expired_array[this.getPriority()].remove(this);
    		 
        }
    	    	
        if(status == ThreadRunning)
        	pre_empt();                  //Pre-empt Running threads to kill 
        
                
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
      	  if(this != null){
	            
            	if(this.getStatus() >= ThreadWaiting){ //suspending a thread for longer time
            		this.setStatus(this.getStatus() + 1 );
            	}
            	else if(this.getStatus() == ThreadRunning){// running thread set to waiting and its task's current thread 
            		this.setStatus(ThreadWaiting);         //must be set to null
            	    this.getTask().setCurrentThread(null);
            	}

                // Remove the current thread from active array and expired array.
            	active_array[this.getPriority()].remove(this);
                expired_array[this.getPriority()].remove(this);
            	
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
	    	    		    	    	
	    	    	active_array[this.getPriority()].remove(this);
    	    		expired_array[this.getPriority()].remove(this);
	    	    	
    	    		if(this.getPriority() > HIGHEST_PRIORITY)
    	    			this.setPriority(this.getPriority() - 1); //raise priority of thread if appropriate
	    	    		    	    		    	    	
	    	    	expired_array[this.getPriority()].append(this); // put ready thread in expired queue.
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
            	    	    	    	
        if(currentThread != null){
    		
        	active_array[currentThread.getPriority()].remove(currentThread);
    		expired_array[currentThread.getPriority()].remove(currentThread);    		    		
    		
    		if( HTimer.get() < 1 && currentThread.getPriority() < LOWEST_PRIORITY){   // Check if quantum expired and possible to lower priority
        		currentThread.setPriority(currentThread.getPriority() + 1); // if so lower priority of thread.
        	}	
    		    		
    		expired_array[currentThread.getPriority()].append(currentThread); // return thread to expired queue.    		
    		
    	}
    	
        boolean actEmpty= arrayEmpty(active_array);
        boolean expEmpty= arrayEmpty(expired_array);
        
    	if(actEmpty == false){ // if not empty select new thread and run it with appropriate quantum.
    	
    		ThreadCB nextThread= (ThreadCB)selectThread();//toBeNext;
    		MMU.setPTBR(nextThread.getTask().getPageTable());
    		
    		nextThread.setStatus(ThreadRunning); //Change nextThread status.
    		nextThread.getTask().setCurrentThread(nextThread); // Set nextThread's task, so nextThread is the current thread.
       	    	   		
    		if(nextThread.getPriority() <= DEFAULT_PRIORITY)      //set the time quantum appropriate for the priority
    			HTimer.set(40);
    		else if(nextThread.getPriority() > DEFAULT_PRIORITY)
    		    HTimer.set(20);
    		    		    		
    		return SUCCESS;
    	}
    	else if (actEmpty== true && expEmpty == false){ 
    	   /* Swap */                                    
           GenericList[] temp= active_array; // swap the address of the arrays if active array empty,             
           active_array= expired_array;   // but expired is still has threads.
           expired_array= temp;
    	            
	       ThreadCB nextThread= (ThreadCB)selectThread();
	   	   MMU.setPTBR(nextThread.getTask().getPageTable());
	   	   
	   	   nextThread.setStatus(ThreadRunning); //Change nextThread status.
	   	   nextThread.getTask().setCurrentThread(nextThread); // Set nextThread's task, so nextThread is the current thread.
	   	   	   	   	   
		   if(nextThread.getPriority() <= DEFAULT_PRIORITY)  //Select The appropriate time quantum for the priority
	   		   HTimer.set(40);
	   	   else if(nextThread.getPriority() > DEFAULT_PRIORITY)
	   		   HTimer.set(20);
	   		   	   
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
    
    /**pre_empt()- pre-empts a running thread.
     * 
     * @return the Thread that was preempted
     */
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
   
    /**arrayEmpty -tests to see if the given array has any remainingg threads.
     * 
     * @param arg- a array of GenericLists
     * @return boolean 
     */
    private static boolean arrayEmpty(GenericList [] arg){ // determines whether active/expired array is empty.
    	
    	for (int i=0; i < arg.length; i++){
    		if(!arg[i].isEmpty()){
    			return false;
    		}
    	}
    	   	
    	return true;
    }
    
    /**selectThread selects a Thread with the highest priority to run from active_array.
     * 
     * @return the Thread to be run.
     */
    private static Object selectThread(){// Selects Thread with highest priority to run.
    	Object toReturn=null;
    	
    	for(int i=0; i < active_array.length; i++){
    		
    		if(!active_array[i].isEmpty()){
    			toReturn= active_array[i].removeHead(); // the next thread to be run.
    			return toReturn;
    		}
    	}
    	    	
    	return toReturn; // no threads to run return initial value of null.
    }
    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
