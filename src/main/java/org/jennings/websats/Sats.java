/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jennings.websats;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.jennings.mvnsat.Sat;

/**
 *
 * @author david
 */
public class Sats {

    private static HashMap<String, Sat> satNames = null;
    private static HashMap<String, Sat> satNums = null;

    private String loadSats() {
        satNames = new HashMap<>();
        satNums = new HashMap<>();

        String message = "";

        try {
            InputStream pis = getClass().getResourceAsStream("/sats.tle");

            BufferedReader br = new BufferedReader(new InputStreamReader(pis, "UTF-8"));
            String tleHeader;
            while ((tleHeader = br.readLine()) != null) {
                String tleLine1 = br.readLine();
                String tleLine2 = br.readLine();
                Sat sat = new Sat(tleHeader, tleLine1, tleLine2);

                satNames.put(sat.getName(), sat);
                satNums.put(sat.getNum(), sat);

            }

        } catch (Exception e) {
            message = "ERROR" + e.getClass() + ">>" + e.getMessage();
            System.out.println(message);
        }
        return message;
    }

    public boolean isSatNums() {

        if (satNums == null) {
            return false;
        } else {
            return true;
        }
    }

    public Sats() {
        try {
            loadSats();
        } catch (Exception e) {
            satNums = null;
            e.printStackTrace();
        }
    }

    public Sat getSatNum(String num) {
        return satNums.get(num);
    }

    public Sat getSatName(String name) {
        return satNames.get(name);
    }

    /**
     * Input could be specific names or names ending with * Each name is check
     * and appended to the list
     *
     * @param name Comma Sep String of Names
     * @return
     */
    public HashSet<String> getSatsByName(String strNames) throws Exception {
        HashSet<String> sats = new HashSet<>();
        
        String[] names = strNames.split(",");
        String message = "";

        for (String name : names) {
            Sat s = satNames.get(name);
            if (s == null) {
                message += name + ",";
            } else {
                sats.add(s.getNum());
            }

        }
        if (!message.equalsIgnoreCase("")) {
            throw new Exception("Sats not found: " + message.substring(0, message.length() - 1));
        }        

        return sats;

    }

    /**
     * Input is comma sep list of nums
     *
     * @param nums
     * @return
     */
    public HashSet<String> getSatsByNum(String strNums) throws Exception {
        HashSet<String> sats = new HashSet<>();

        String[] nums = strNums.split(",");
        String message = "";

        for (String num : nums) {
            Sat s = satNums.get(num);
            if (s == null) {
                message += num + ",";
            } else {
                sats.add(s.getNum());
            }

        }
        if (!message.equalsIgnoreCase("")) {
            throw new Exception("Sats not found: " + message.substring(0, message.length() - 1));
        }

        return sats;

    }

    public HashSet<String> getAllNums() {
        HashSet<String> sats = new HashSet<>();

        satNums.keySet().stream().forEach((s) -> {
            sats.add(s);
        });

        return sats;

    }

    public static void main(String[] args) {
        Sats t = new Sats();

        satNums.keySet().stream().forEach((s) -> {
            System.out.println(s);
        });

////        HashSet<String> s = new HashSet<>() ;
////        s.add("David");
////        
////        s.add("David");
////        s.add("Colleen");
////        
////       
//        
//        for (String a: s) {
//            System.out.println(a);
//        }
    }

}
