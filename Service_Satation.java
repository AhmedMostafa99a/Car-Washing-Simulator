import java.util.*;

class Semaphore {
    protected int value = 0;

    protected Semaphore() {
        value = 0;
    }

    protected Semaphore(int initial) {
        value = initial;
    }

    // P (decreas / wait)
    public synchronized void decreas() {
        value--;
        if (value < 0)
            try {
                wait();
            } catch (InterruptedException e) {
            }
    }

    // V (increas / signal)
    public synchronized void increas() {
        value++;
        if (value <= 0)
            notify();
    }

    public synchronized int get_value() {
        return value;
    }
}

// ================================================
// Car [Producer]
// ================================================
class Car extends Thread {
    private ServiceStation serv;
    private String name;

    public Car(ServiceStation s, String name) {
        this.name = name;
        serv = s;
    }

    public void add_car() {
        serv.Car_spaces.decreas();

        serv.metux.decreas();

        serv.store[serv.inptr] = name;
        serv.inptr = (serv.inptr + 1) % serv.get_size();
        serv.metux.increas();

        serv.Car_elements.increas();
    }

    public void run() {
        System.out.println(name + " arrived");

        try {
            Thread.sleep(500 + ServiceStation.rand.nextInt(1501));
        } catch (InterruptedException e) {
        }

        if (serv.pumbs.get_value() == 0) {
            System.out.println(name + " arrived and waiting");
        }
        add_car();
    }
}

// ================================================
// Pump [Consumer]
// ================================================
class PumP extends Thread {
    public ServiceStation serv;
    public String Name;
    public int bay;

    public PumP(ServiceStation s, String name, int num_bay) {
        Name = name;
        serv = s;
        bay = num_bay;
    }

    public void run() {
        while (true) {

            serv.Car_elements.decreas();

            serv.metux.decreas();

            String Car_name = (String) serv.store[serv.outptr];
            serv.outptr = (serv.outptr + 1) % serv.get_size();

            System.out.println(Name + ": " + Car_name + " Occupied");

            serv.metux.increas();

            serv.Car_spaces.increas();

            serv.pumbs.decreas();

            System.out.println(Name + ":" + Car_name + " login");
            System.out.println(Name + ": " + Car_name + " begins service at Bay " + bay);
            try {
                Thread.sleep(3000 + ServiceStation.rand.nextInt(2001));
            } catch (InterruptedException e) {
            }
            System.out.println(Name + ": " + Car_name + " finishes service");

            serv.pumbs.increas();

            System.out.println(Name + ": Bay " + bay + " is now free");
        }
    }
}

// ================================================
// ServiceStation
// ================================================
public class ServiceStation {

    private int size;
    protected Object store[];
    protected int inptr = 0;
    protected int outptr = 0;
    public static Random rand = new Random();

    protected Semaphore Car_spaces; // "empty"
    protected Semaphore Car_elements; // "full"
    protected Semaphore metux; // "mutex"
    public Semaphore pumbs; // Counts available service bays

    public ServiceStation(int s, int num_pumps) {
        size = s;
        store = new Object[size];

        Car_spaces = new Semaphore(size);
        Car_elements = new Semaphore(0);
        pumbs = new Semaphore(num_pumps);
        metux = new Semaphore(1);
    }

    public int get_size() {
        return size;
    }

    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);

        System.out.print("Waiting area capacity: ");
        int size = sc.nextInt();
        while (size < 1 || size > 10) {
            System.out.print("Please enter a positive integer for capacity: ");
            size = sc.nextInt();
        }
        System.out.print("Number of service bays (pumps): ");
        int numPumps = sc.nextInt();
        sc.nextLine(); // clear buffer
        System.out.print("Cars arriving (order): ");
        String[] cars = sc.nextLine().split(" ");

        ServiceStation serv = new ServiceStation(size, numPumps);

        Thread[] pumps = new Thread[numPumps];
        for (int i = 0; i < numPumps; i++) {
            pumps[i] = new PumP(serv, "Pump " + (i + 1), i + 1);
            pumps[i].setDaemon(true);
            pumps[i].start();
        }

        Thread[] carThreads = new Thread[cars.length];
        for (int i = 0; i < cars.length; i++) {
            carThreads[i] = new Car(serv, cars[i]);
            carThreads[i].start();
            Thread.sleep(800);
        }

        // Wait for all Car threads to finish
        for (Thread t : carThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {

            }
        }

        // --- Correct Shutdown Logic ---
        while (serv.Car_elements.get_value() > 0 || serv.pumbs.get_value() < numPumps) {
            Thread.sleep(100);
        }

        System.out.println("All cars processed; simulation ends");
        sc.close();
    }
}
