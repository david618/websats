/*
* To run in Jetty 
* Add to webapps Folder of Jetty
* java -jar start.jar -Djetty.port=9999
 */
package org.jennings.websats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jennings.mvnsat.Sat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 */
@WebServlet(name = "satellites", urlPatterns = {"/satellites"})
public class satellites extends HttpServlet {

    private static HashMap<String, Sat> satNames = null;
    private static HashMap<String, Sat> satNums = null;

    private String loadSats() {
        satNames = new HashMap<>();
        satNums = new HashMap<>();

        String message = null;

        try {
            InputStream pis = getClass().getResourceAsStream("/sats.tle");

            BufferedReader br = new BufferedReader(new InputStreamReader(pis, "UTF-8"));
            String tleHeader = null;
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

        if (satNames == null || satNums == null) {
            String msg = loadSats();
            if (msg != null) {
                response.setContentType("text/html;charset=UTF-8");

                try (PrintWriter out = response.getWriter()) {
                    /* TODO output your page here. You may use following sample code. */
                    out.println("<!DOCTYPE html>");
                    out.println("<html>");
                    out.println("<head>");
                    out.println("<title>Error Loading Satelites</title>");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("<h1>Could not load satellites</h1>");
                    out.println("</body>");
                    out.println("</html>");
                }

            }
        }

        String strFormat = "text";
        String strNums = "*";
        String strNames = "*";
        String strTime = "";
        boolean isNums = false;

        // Populate the parameters ignoring case
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            if (paramName.equalsIgnoreCase("f")) {
                strFormat = request.getParameter(paramName);
            } else if (paramName.equalsIgnoreCase("t")) {
                strTime = request.getParameter(paramName);
            } else if (paramName.equalsIgnoreCase("names")) {
                strNames = request.getParameter(paramName);
            } else if (paramName.equalsIgnoreCase("nums")) {
                strNums = request.getParameter(paramName);
                isNums = true;
            }
        }

        try {

            String[] satArray = null;

            long t = System.currentTimeMillis();  // Default to current system time
            if (!strTime.equalsIgnoreCase("")) {
                t = Long.parseLong(strTime);
                if (t < 10000000000L) {
                    // Assume it seconds convert to ms
                    t = t * 1000;
                }
            }            
            
            
            // Nums trumps names
            if (isNums) {
                if (strNums.equalsIgnoreCase("*")) {
                    satArray = satNums.keySet().toArray(new String[0]);
                } else {
                    satArray = strNums.split(",");
                }
            } else if (strNames.equalsIgnoreCase("*")) {
                satArray = satNames.keySet().toArray(new String[0]);
            } else {
                satArray = strNames.split(",");
            }

            int numSats = satArray.length;

            if (strFormat.equalsIgnoreCase("json") || strFormat.equalsIgnoreCase("pjson")) {
                // Return Json
                JSONArray jsonSatArray = new JSONArray();
                JSONObject jsonSat = new JSONObject();

                response.setContentType("application/json;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    

                    for (String sat : satArray) {
                        //System.out.println(sat);
                        Sat pos = null;
                        String message = "";
                        if (isNums) {
                            Sat s = satNums.get(sat);
                            if (s == null) {
                                // No satellite with that num
                                message = "No sattelite with num: " + sat;
                            } else {
                                try {
                                    pos = s.getPos(t);
                                } catch (Exception e) {
                                    // Some will be missed if time is in the past
                                }
                            }

                        } else {
                            Sat s = satNames.get(sat);
                            if (s == null) {
                                // No satellite with that Name
                                message = "No sattelite with name: " + sat;
                            } else {
                                try {
                                    pos = s.getPos(t);
                                } catch (Exception e) {
                                    // Some will be missed if time is in the past
                                }

                            }

                        }

                        JSONObject json = new JSONObject();
                        if (pos == null) {
                            json.put("error", message);
                        } else {
                            json.put("name", pos.getName());
                            json.put("num", pos.getNum());
                            json.put("timestamp", pos.GetEpoch().epochTimeSecs());
                            json.put("dtg", pos.GetEpoch());
                            json.put("lon", pos.GetLon());
                            json.put("lat", pos.GetParametricLat());
                            json.put("alt", pos.getAltitude());

                        }

                        if (numSats == 1) {
                            jsonSat = json;
                        }

                        jsonSatArray.put(json);

                    }

                    if (strFormat.equalsIgnoreCase("json")) {
                        if (numSats == 1) {
                            out.println(jsonSat.toString());
                        } else {
                            out.println(jsonSatArray.toString());
                        }
                    } else if (numSats == 1) {
                        out.println(jsonSat.toString(2));
                    } else {
                        out.println(jsonSatArray.toString(2));
                    }

                }
            } else if (strFormat.equalsIgnoreCase("geojson")) {
                // Standard GeoJSON Outuput

                JSONObject featureCollection = new JSONObject();
                featureCollection.put("type", "FeatureCollection");

                JSONArray features = new JSONArray();

                response.setContentType("application/json;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    

                    for (String sat : satArray) {
                        //System.out.println(sat);
                        Sat pos = null;
                        String message = "";
                        if (isNums) {
                            Sat s = satNums.get(sat);
                            if (s == null) {
                                // No satellite with that num
                                message = "No sattelite with num: " + sat;
                            } else {
                                try {
                                    pos = s.getPos(t);
                                } catch (Exception e) {
                                    // Some will be missed if time is in the past
                                }
                            }

                        } else {
                            Sat s = satNames.get(sat);
                            if (s == null) {
                                // No satellite with that Name
                                message = "No sattelite with name: " + sat;
                            } else {
                                try {
                                    pos = s.getPos(t);
                                } catch (Exception e) {
                                    // Some will be missed if time is in the past
                                }

                            }

                        }

                        JSONObject feature = new JSONObject();
                        feature.put("type", "Feature");
                        
                        JSONObject properties = new JSONObject();
                        
                        if (pos == null) {
                            // Not sure how to handle errors yet for gjson
                        } else {
                            properties.put("name", pos.getName());
                            properties.put("num", pos.getNum());
                            properties.put("timestamp", pos.GetEpoch().epochTimeSecs());
                            properties.put("dtg", pos.GetEpoch());
                            properties.put("lon", pos.GetLon());
                            properties.put("lat", pos.GetParametricLat());
                            properties.put("alt", pos.getAltitude());
                            feature.put("properties", properties);
                            
                            JSONObject geom = new JSONObject();
                            geom.put("type", "Point");
                            JSONArray coord = new JSONArray("[" + pos.GetLon() + ", " + pos.GetParametricLat()+ "]");
                            geom.put("coordinates",coord);                            
                            feature.put("geometry", geom);
                            
                            features.put(feature);
                            
                        }

                        

                    }
                
                    featureCollection.put("features",features);                           

                    out.println(featureCollection);
                    
                }

            } else {
                // Return Text
                response.setContentType("text/plain;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    

                    for (String sat : satArray) {
                        //System.out.println(sat);                        
                        String message = null;
                        Sat pos;
                        if (isNums) {
                            pos = satNums.get(sat);

                        } else {
                            pos = satNames.get(sat);
                        }

                        String strLine = null;
                        if (pos == null) {
                            strLine = "# No satellite : " + sat;

                        } else {
                            try {
                                pos = pos.getPos(t);
                                strLine = pos.getName() + "|" + pos.getNum() + "|" + pos.GetEpoch().epochTimeSecs() + "|"
                                        + pos.GetEpoch() + "|" + pos.GetLon() + "|" + pos.GetParametricLat()
                                        + "|" + pos.getAltitude();
                            } catch (Exception e) {
                                // Skipping errors for now
                            }
                        }

                        out.println(strLine);

                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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
