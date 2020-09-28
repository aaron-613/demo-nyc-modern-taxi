package com.solace.demo.taxi;

import java.util.concurrent.atomic.AtomicInteger;

public class Driver {
    
    private static AtomicInteger nextDriverId = new AtomicInteger(1);
    
    public enum State {
        IDLE,
        OCCUPIED,
        ;
    }
    
    
    private final long id;  // 8 digit integer: 00000000 .. 99999999
    private State state = State.IDLE;
    private final String firstName;
    private final String lastName;
    private final String carClass;
    private int totalTrips;
    private int totalRatings;  // sum of all ratings, which are 0..5
    
    
    // https://gist.github.com/benhorgen/4494868
//    private static final String[] FIRSTS =  new String[] { "Adam", "Alex", "Aaron", "Ben", "Carl", "Dan", "David", "Edward", "Fred", "Frank", "George", "Hal", "Hank", "Ike", "John", "Jack", "Joe", "Larry", "Monte", "Matthew", "Mark", "Nathan", "Otto", "Paul", "Peter", "Roger", "Roger", "Steve", "Thomas", "Tim", "Ty", "Victor", "Walter"};
    private static final String[] LASTS =  new String[] { "Anderson", "Ashwoon", "Aikin", "Bateman", "Bongard", "Bowers", "Boyd", "Cannon", "Cast", "Deitz", "Dewalt", "Ebner", "Frick", "Hancock", "Haworth", "Hesch", "Hoffman", "Kassing", "Knutson", "Lawless", "Lawicki", "Mccord", "McCormack", "Miller", "Myers", "Nugent", "Ortiz", "Orwig", "Ory", "Paiser", "Pak", "Pettigrew", "Quinn", "Quizoz", "Ramachandran", "Resnick", "Sagar", "Schickowski", "Schiebel", "Sellon", "Severson", "Shaffer", "Solberg", "Soloman", "Sonderling", "Soukup", "Soulis", "Stahl", "Sweeney", "Tandy", "Trebil", "Trusela", "Trussel", "Turco", "Uddin", "Uflan", "Ulrich", "Upson", "Vader", "Vail", "Valente", "Van Zandt", "Vanderpoel", "Ventotla", "Vogal", "Wagle", "Wagner", "Wakefield", "Weinstein", "Weiss", "Woo", "Yang", "Yates", "Yocum", "Zeaser", "Zeller", "Ziegler", "Bauer", "Baxter", "Casal", "Cataldi", "Caswell", "Celedon", "Chambers", "Chapman", "Christensen", "Darnell", "Davidson", "Davis", "DeLorenzo", "Dinkins", "Doran", "Dugelman", "Dugan", "Duffman", "Eastman", "Ferro", "Ferry", "Fletcher", "Fietzer", "Hylan", "Hydinger", "Illingsworth", "Ingram", "Irwin", "Jagtap", "Jenson", "Johnson", "Johnsen", "Jones", "Jurgenson", "Kalleg", "Kaskel", "Keller", "Leisinger", "LePage", "Lewis", "Linde", "Lulloff", "Maki", "Martin", "McGinnis", "Mills", "Moody", "Moore", "Napier", "Nelson", "Norquist", "Nuttle", "Olson", "Ostrander", "Reamer", "Reardon", "Reyes", "Rice", "Ripka", "Roberts", "Rogers", "Root", "Sandstrom", "Sawyer", "Schlicht", "Schmitt", "Schwager", "Schutz", "Schuster", "Tapia", "Thompson", "Tiernan", "Tisler" };

    // https://gist.github.com/brysonian/99430
    private static final String[] FIRSTS =  new String[] {  "Allison", "Arthur", "Ana", "Alex", "Arlene", "Alberto", "Barry", "Bertha", "Bill", "Bonnie", "Bret", "Beryl", "Chantal", "Cristobal", "Claudette", "Charley", "Cindy", "Chris", "Dean", "Dolly", "Danny", "Danielle", "Dennis", "Debby", "Erin", "Edouard", "Erika", "Earl", "Emily", "Ernesto", "Felix", "Fay", "Fabian", "Frances", "Franklin", "Florence", "Gabielle", "Gustav", "Grace", "Gaston", "Gert", "Gordon", "Humberto", "Hanna", "Henri", "Hermine", "Harvey", "Helene", "Iris", "Isidore", "Isabel", "Ivan", "Irene", "Isaac", "Jerry", "Josephine", "Juan", "Jeanne", "Jose", "Joyce", "Karen", "Kyle", "Kate", "Karl", "Katrina", "Kirk", "Lorenzo", "Lili", "Larry", "Lisa", "Lee", "Leslie", "Michelle", "Marco", "Mindy", "Maria", "Michael", "Noel", "Nana", "Nicholas", "Nicole", "Nate", "Nadine", "Olga", "Omar", "Odette", "Otto", "Ophelia", "Oscar", "Pablo", "Paloma", "Peter", "Paula", "Philippe", "Patty", "Rebekah", "Rene", "Rose", "Richard", "Rita", "Rafael", "Sebastien", "Sally", "Sam", "Shary", "Stan", "Sandy", "Tanya", "Teddy", "Teresa", "Tomas", "Tammy", "Tony", "Van", "Vicky", "Victor", "Virginie", "Vince", "Valerie", "Wendy", "Wilfred", "Wanda", "Walter", "Wilma", "William", "Kumiko", "Aki", "Miharu", "Chiaki", "Michiyo", "Itoe", "Nanaho", "Reina", "Emi", "Yumi", "Ayumi", "Kaori", "Sayuri", "Rie", "Miyuki", "Hitomi", "Naoko", "Miwa", "Etsuko", "Akane", "Kazuko", "Miyako", "Youko", "Sachiko", "Mieko", "Toshie", "Junko"};

    public static final String[] CAR_TYPES = new String[] { "Sedan", "SUV", "Minivan", "Coupe" };
    
    private Driver(long id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.carClass = CAR_TYPES[(int)(Math.random()*CAR_TYPES.length)];
        this.totalTrips = (int)(Math.random()*5000);
        this.totalRatings = (int)(((Math.random()*3.0) + 2) *totalTrips);  // should be between 2-5
    }
    
    public static Driver newInstance() {
        long id = nextDriverId.getAndIncrement();
        //String id = ""+(int)(Math.random()*100_000_000);
        String first = FIRSTS[(int)(Math.random()*FIRSTS.length)];
        String last = LASTS[(int)(Math.random()*LASTS.length)];
        return new Driver(id,first,last);
    }

    public long getId() {
        return id;
    }
    
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }
    
    public String getName() {
        return firstName + " " + lastName;
    }
    
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }


    public String getCarClass() {
        return carClass;
    }
    
    public int getTotalTrips() {
        return totalTrips;
    }
    
    /**
     * Returns rating, rounded to 2 decimals
     */
    public double getRating() {
        return Math.round(totalRatings * 100.0 / totalTrips) / 100.0;  // round to 2 decimal places
    }
    
    @Override
    public String toString() {
        return String.format("%s %s, #%s, %.2f (%s)",getFirstName(),getLastName(),getId(),getRating(),getCarClass());
    }
    
    
    public static void main(String... args) {
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
        System.out.println(Driver.newInstance());
    }
    
    
}
