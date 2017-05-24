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
//import

public class SwapTest {
    static GenericList [] act;
    static GenericList[] exp;
	static GenericList temp;
	private static Object selectThread(GenericList [] arg){
    	Object toReturn=null;
    	
    	for(int i=0; i < arg.length; i++){
    		
    		if(!arg[i].isEmpty()){
    			toReturn= arg[i].removeHead(); // the next thread to be run.
    			//expired_array[i].remove(toReturn);     // remove from expired just in case.
    			
    			return toReturn;
    		}
    	}
    	    	
    	return toReturn; // no threads to run
    }
	private static boolean arrayEmpty(GenericList [] arg){
	    	
	    	for (int i=0; i < arg.length; i++){
	    		if(!arg[i].isEmpty()){
	    			return false;
	    		}
	    	}
	    	   	
	    	return true;
	    }  
	public static void main(String[] args) {
	
		act= new GenericList[1];
		act[0]= new GenericList();
		
		exp= new GenericList[1];
		exp[0]= new GenericList();
		
		for(int i=0; i < 3; i++)
			act[0].append(i);
		
		for(int i=3; i < 7; i++)
			exp[0].append(i);
		
//		System.out.println("act");
//		while(!act[0].isEmpty()){
//			   
//			   System.out.println( (Integer)act[0].removeHead());
//			
//		}
//		System.out.println("\nexp");
//		while(!exp[0].isEmpty()){
//			   
//			   System.out.println( (Integer)exp[0].removeHead());
//		}
		
		System.out.println("SWAP!");//Swap act w/ exp
		temp = act[0];
		act[0]= exp[0];
		
	    exp[0]= temp;
	    
//	    while(!temp.isEmpty())
//	    	temp.removeHead();
//	    temp = act[0];
//	   System.out.println("act");
//	   while(!act[0].isEmpty()){
//		    System.out.println("array Empty on act"+arrayEmpty(act));
//		   System.out.println( (Integer)act[0].removeHead());
//		
//	   }
//	   System.out.println("\nexp");
//	   System.out.println("array Empty on exp"+arrayEmpty(exp));
//	   while(!exp[0].isEmpty()){
//		  
//		   System.out.println( (Integer)exp[0].removeHead());
//		
//	   }
	   
	  System.out.println(selectThread(act));
	  System.out.println(selectThread(act));
	  System.out.println(selectThread(act));
	  System.out.println(selectThread(act));
	  System.out.println(selectThread(act));
	  //selectThread(act); 
	   
	}

}
