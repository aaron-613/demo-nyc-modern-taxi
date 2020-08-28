package com.solace.demo.taxi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Names {
    
    // STATIC INIT /////////////////////////////////

    private static final List<String> FIRST_NAMES;
    private static final List<String> LAST_NAMES;
    
    static {
        FIRST_NAMES = load("firstNames.txt");
        LAST_NAMES = load("lastnames.txt");
    }
    
    private static List<String> load(String filename) {
        List<String> list = new ArrayList<>();
        BufferedReader reader = null;
        try {
            //reader = new BufferedReader(new FileReader(getClass().getClassLoader().getResource(filename).getFile()));
            reader = new BufferedReader(new InputStreamReader(Names.class.getClassLoader().getResourceAsStream(filename)));  // when using get class loader, cannot include leading / in filename!
        } catch (NullPointerException e) {
            System.out.println("Tried to load "+filename+" but couldn't be found!");
            throw e;
        }
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue;
                list.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
        return list;
    }
    // STATIC INIT /////////////////////////////////
    
    private Names() {
        throw new AssertionError("Don't instantiate this class");
    }
    
    public static String randomFirstName() {
        return FIRST_NAMES.get((int)(Math.random()*FIRST_NAMES.size()));
    }

    public static String randomLastName() {
        return LAST_NAMES.get((int)(Math.random()*LAST_NAMES.size()));
    }
    
    
}
