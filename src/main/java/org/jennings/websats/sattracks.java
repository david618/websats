/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jennings.websats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jennings.geomtools.GeographicCoordinate;
import org.jennings.mvnsat.Sat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class sattracks extends HttpServlet {

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

    
    private GeographicCoordinate findCrossing(double x1, double y1, double x2, double y2) {
        GeographicCoordinate pt;
        
        double m = (y2-y1)/(x2-x1);
        
        double latDT = m*(180.0 - x1) + y1;
        
        return new GeographicCoordinate(180.0, latDT);
        
    }
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String message = "";

        try {
            if (satNames == null || satNums == null) {
                message = loadSats();

                if (!message.equalsIgnoreCase("")) {

                    // Failed to load satellite lists
                    throw new Exception(message);
                }
            }

            String strFormat = "txt";
            String strGeomType = "esri";
            String strNums = "*";
            String strNames = "*";
            String strTimeStart = "";
            String strTimeDuration = "";
            String strTimeStep = "";
            boolean isNums = false;

            // Populate the parameters ignoring case
            Enumeration paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                if (paramName.equalsIgnoreCase("f")) {
                    strFormat = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("gt")) {
                    strGeomType = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("st")) {
                    strTimeStart = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("td")) {
                    strTimeDuration = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("ts")) {
                    strTimeStep = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("names")) {
                    strNames = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("nums")) {
                    strNums = request.getParameter(paramName);
                    isNums = true;
                }
            }

            String[] satInput;
            ArrayList<String> sats = new ArrayList<>();

            long tstart = System.currentTimeMillis();  // Default to current system time
            if (!strTimeStart.equalsIgnoreCase("")) {
                tstart = Long.parseLong(strTimeStart);
                if (tstart < 10000000000L) {
                    // Assume it seconds convert to ms
                    tstart = tstart * 1000;
                }
            }

            int tduration = 300;  // Default to 5 minutes
            if (!strTimeDuration.equalsIgnoreCase("")) {
                tduration = Integer.parseInt(strTimeDuration);
            }

            int tstep = 60; // Default to 1 sample per min
            if (!strTimeStep.equalsIgnoreCase("")) {
                tstep = Integer.parseInt(strTimeStep);
            }

            // Create a list of satellites to return
            if (!strNames.equalsIgnoreCase("*") || !strNums.equalsIgnoreCase("*")) {
                // User has specified a list
                if (isNums) {
                    satInput = strNums.split(",");
                } else {
                    satInput = strNames.split(",");
                }

                for (String sat : satInput) {
                    if (isNums) {
                        Sat s = satNums.get(sat);
                        if (s == null) {
                            message += sat + ",";
                        } else {
                            sats.add(s.getNum());
                        }
                    } else {
                        Sat s = satNames.get(sat);
                        if (s == null) {
                            message += sat + ",";
                        } else {
                            sats.add(s.getNum());
                        }
                    }
                    if (!message.equalsIgnoreCase("")) {
                        throw new Exception(message.substring(0, message.length() - 1));
                    }
                }

            } else {
                // No list specified; return all                
                //satInput = satNums.keySet().toArray(new String[0]);
                satNums.keySet().stream().forEach((s) -> {
                    sats.add(s);
                });

            }

            JSONArray results = new JSONArray();
            String strLines = "";

            // Process the list of satellites
            for (String sat : sats) {
                Sat pos = satNums.get(sat).getPos(tstart);
                JSONObject result = new JSONObject();
                JSONArray line = new JSONArray();
                JSONArray lines = new JSONArray();
                
                try {
                    long t1 = tstart;
                    pos = satNums.get(sat).getPos(t1);
                    double lat1 = pos.GetParametricLat();
                    double lon1 = pos.GetLon();
                    while (t1 <= tstart + tduration * 1000) {
                        pos = satNums.get(sat).getPos(t1);
                        double lat2 = pos.GetParametricLat();
                        double lon2 = pos.GetLon();
                        
                        
                        // If you cross the DateTime line then inject a break at 180
                        if (Math.abs(lon2 - lon1) > 180.0) {
                            System.out.println("");
                            System.out.println(pos.getName());
                            System.out.println(lon1 + "," + lat1);
                            System.out.println("Break");
                            System.out.println(lon2 + "," + lat2);
                            double x1 = lon1;
                            double x2 = lon2;
                            if (lon1 < 0) x1 += 360.0;
                            if (lon2 < 0) x2 += 360.0;
                            GeographicCoordinate crossing = findCrossing(x1, lat1, x2, lat2);
                            System.out.println(crossing);
                            
                            if (lon1 < 0) {
                                // Insert with -180
                                JSONArray coord = new JSONArray("[-180, " + crossing.getLat() + "]");
                                line.put(coord);                                
                            } else {
                                // Insert with 180
                                JSONArray coord = new JSONArray("[180, " + crossing.getLat() + "]");
                                line.put(coord);                                
                            }
                            // Insert line and start new line
                            lines.put(line);
                            line = new JSONArray();
                            
                            if (lon2 < 0) {
                                // Start with -180
                                JSONArray coord = new JSONArray("[-180, " + crossing.getLat() + "]");
                                line.put(coord);                                
                            } else {
                                // Start with 180
                                JSONArray coord = new JSONArray("[180, " + crossing.getLat() + "]");
                                line.put(coord);                                
                            }
                            
                            
                        }
                        
                        JSONArray coord = new JSONArray("[" + pos.GetLon() + ", " + pos.GetParametricLat() + "]");
                        line.put(coord);
                        
                        lat1 = lat2;
                        lon1 = lon2;
                        
                        t1 += tstep * 1000;
                    }
                    lines.put(line);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (strFormat.equalsIgnoreCase("geojson")) {
                    
                    result.put("type", "Feature");
                    
                    JSONObject properties = new JSONObject();
                    properties.put("name", pos.getName());
                    properties.put("num", sat);
                    result.put("properties", properties);

                    JSONObject geom = new JSONObject();
                    geom.put("type", "MultiLineString");
                    geom.put("coordinates", lines);
                    result.put("geometry", geom);

                    results.put(result);
                } else if (strFormat.equalsIgnoreCase("json") || strFormat.equalsIgnoreCase("txt")) {
                                                            
                    
                    JSONObject geom2 = new JSONObject();
                    if (strGeomType.equalsIgnoreCase("geojson")) {                        
                        geom2.put("coordinates", line);

                    } else {
                        geom2.put("paths", line);
                    }                    

                    if (strFormat.equalsIgnoreCase("json")) {
                        result.put("name", pos.getName());
                        result.put("num", sat);
                        result.put("geometry", geom2);
                        results.put(result);
                        
                    } else {
                        // Default to delimited
                        strLines += pos.getName() + "|" + pos.getNum() + "|" +
                                geom2.toString() + "\n";
                        
                    }
                    
                } else {
                    message = "Invalid format. Supported formats: txt,json,geojson";
                    throw new Exception();
                }

            }

            JSONObject resp = new JSONObject();

            PrintWriter out = response.getWriter();
                        
            if (strFormat.equalsIgnoreCase("geojson")) {
                response.setContentType("application/json;charset=UTF-8");
                JSONObject featureCollection = new JSONObject();
                featureCollection.put("type", "FeatureCollection");

                featureCollection.put("features", results);

                out.println(featureCollection);

            } else if (strFormat.equalsIgnoreCase("json")) {
                response.setContentType("application/json;charset=UTF-8");
                resp.put("startTime", tstart);
                resp.put("duration", tduration);
                resp.put("step", tstep);
                resp.put("sats", results);

                out.println(resp.toString());

            } else {
                // Pipe Delimited
                response.setContentType("text/plain;charset=UTF-8");
                out.println(strLines);
            }

        } catch (Exception e) {
            try (PrintWriter out = response.getWriter()) {
                /* TODO output your page here. You may use following sample code. */
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("<head>");
                out.println("<title>Error Creating Satellite Array</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("<h1>Could build satellite array.</h1>");
                if (!message.equalsIgnoreCase("")) {
                    out.println("<h2>Error: " + message + "</h2>");
                } else {
                    out.println("<h2>Unexpected Error: " + e.getMessage() + "</h2>");
                }
                out.println("</body>");
                out.println("</html>");
            }
        }

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
