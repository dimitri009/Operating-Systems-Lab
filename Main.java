
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 *
 * @author dimit
 */

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        int K=20;
        int T1=100;
        int T2=50;
        ResourceManager rm = new ResourceManager(20, 20);
        RichiesteQueue rq = new RichiesteQueue(K);
        
        ClientThread[] ct = new ClientThread[5];
        for (int i=0;i<ct.length;i++){
            ct[i]=new ClientThread(rq);
            ct[i].setName("Client "+i);
            ct[i].start();
        }
        
        WorkerThread[] wt = new WorkerThread[3];
        for(int i=0;i<wt.length;i++){
            wt[i]= new WorkerThread(rq, T1, T2, rm);
            wt[i].setName("Worker "+i);
            wt[i].start();
        }
        
        Thread.sleep(10000);
        for (int i=0;i<wt.length;i++){
            wt[i].interrupt();
        }
        
        for (int i=0; i<ct.length;i++){
            ct[i].interrupt();
            ct[i].join();
        }
        
        for (ClientThread ct1 : ct) {
            System.out.println(ct1.getName() + " tempo minimo :" + ct1.min + ", tempo massimo:" + ct1.max + " tempo medio:" + (ct1.avg / ct1.count));
        }
        System.out.println("ra:"+rm);
    }
    
}

class ResourceManager {
    private int nA;
    private int nB;
    private Semaphore mutex = new Semaphore(1);
    private Semaphore vuoteA = new Semaphore(0);
    private Semaphore vuoteB = new Semaphore(0);
    
    public ResourceManager(int nA, int nB) {
        this.nA = nA;
        this.nB = nB;
    }
    
    public void acquireA()throws InterruptedException{
        mutex.acquire();
        nA--;
        mutex.release();
        vuoteA.release();
    }
    
    public void acquireB()throws InterruptedException{
        mutex.acquire();
        nB--;
        mutex.release();
        vuoteB.release();
    }
    
    public void releaseA()throws InterruptedException{
        vuoteA.acquire();
        mutex.acquire();
        nA++;
        mutex.release();
    }
    
    public void releaseB()throws InterruptedException{
        vuoteB.acquire();
        mutex.acquire();
        nB++;
        mutex.release();
    }
    
    @Override
    public String toString (){
        return "nA "+nA+", nB "+nB;
    }
}
class Richiesta {
     float value;
     boolean set = false;

    public Richiesta(float value) {
        this.value = value;
    }
}

class RichiesteQueue {
    private ArrayList<Richiesta> queue = new ArrayList<>();
    private int K;
    private Semaphore mutex = new Semaphore(1);
    private Semaphore pieni = new Semaphore(0);
    private Semaphore vuote;
    private Semaphore set = new Semaphore(1);

    public RichiesteQueue(int K) {
        this.K = K;
        this.vuote = new Semaphore(K);
    }
    
    public void getRichieste(Richiesta r) throws InterruptedException{
        vuote.acquire();
        set.acquire();
        mutex.acquire();
        queue.add(r);
        mutex.release();
        pieni.release();
    }
    
    public void waitRisp () throws InterruptedException{ 
        set.acquire();
        set.release();
        
    }
    public void setRichiesta ()throws InterruptedException{
        pieni.acquire();
        mutex.acquire();
        float v = queue.get(0).value;
        queue.get(0).value= v*2; 
        queue.get(0).set=true;
        queue.remove(0);
        mutex.release();
        set.release();
        vuote.release();
    }
}

class ClientThread extends Thread{
    private RichiesteQueue rq;
    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE; 
    long avg;
    int count; 

    public ClientThread(RichiesteQueue rq) {
        this.rq = rq;
    }
    
    public void run (){
        try{
            while(true){
                float random = (float)(Math.random()*99);
                Richiesta r= new Richiesta(random);
                long t1 = System.currentTimeMillis();
                rq.getRichieste(r);
                rq.waitRisp();
                long t2 = System.currentTimeMillis();
                long tempo = t2-t1;
                if (min>tempo)
                    min=tempo;
                if (max<tempo)
                    max=tempo;
                avg+=tempo;
                count++;
                System.out.println(getName()+" valore inviato :"+random+", valore ricevuto :"+r.value+", Took : " + ((tempo) /*/ 1000*/+" millisecondi."));
                
            }
        }catch (InterruptedException e){
            System.out.println(getName()+" Interrotto !");
        }
    }
    
}

class WorkerThread extends Thread {
    private RichiesteQueue rq;
    private int T1;
    private int T2;
    private ResourceManager rm;

    public WorkerThread(RichiesteQueue rq, int T1, int T2, ResourceManager rm) {
        this.rq = rq;
        this.T1 = T1;
        this.T2 = T2;
        this.rm = rm;
    }
    
    public void run (){
        try{
            while(true){
               rm.acquireA();
               rm.acquireB();
               rq.setRichiesta();
               try{sleep(T1);  
                   sleep(T2);                
               }finally{
                   rm.releaseA();
                   rm.releaseB();}
            }
            
        }catch (InterruptedException e){
            System.out.println(getName()+" Interrotto !");
        }
    }
}
