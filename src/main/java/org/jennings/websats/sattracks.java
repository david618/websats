/**
 * sattracks returns lines which 2 or more satellite positions
 */
package org.jennings.websats;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
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

    private static Sats satDB = null;
    
    private GeographicCoordinate findCrossing(double x1, double y1, double x2, double y2) {
        GeographicCoordinate pt;

        double m = (y2 - y1) / (x2 - x1);

        double latDT = m * (180.0 - x1) + y1;

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

        try {
            if (satDB == null) {
                satDB = new Sats();
            }

            String strFormat = "txt";
            String strGeomType = "";
            String strNums = "";
            String strNames = "";
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

            long tstart = System.currentTimeMillis() - 60000;  // Default to current system time
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

            HashSet<String> sats = new HashSet<>();

            if (strNames.equalsIgnoreCase("") && strNums.equalsIgnoreCase("")) {
                sats = satDB.getAllNums();
            }

            if (!strNums.equalsIgnoreCase("")) {
                // Nums were specified 
                sats = satDB.getSatsByNum(strNums);
            } else if (!strNames.equalsIgnoreCase("")) {
                // Names were specified
                sats = satDB.getSatsByName(strNames);
            }

            JSONArray results = new JSONArray();
            String strLines = "";
            
            PrintWriter out = response.getWriter();

            // Process the list of satellites
            for (String sat : sats) {
                Sat st = satDB.getSatNum(sat);
                Sat pos = st.getPos(tstart);
                JSONObject result = new JSONObject();
                JSONArray line = new JSONArray();
                JSONArray lines = new JSONArray();

                try {
                    long t1 = tstart;
                    pos = st.getPos(t1);
                    double lat1 = pos.GetParametricLat();
                    double lon1 = pos.GetLon();
                    while (t1 <= tstart + tduration * 1000) {
                        pos = st.getPos(t1);
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
                            if (lon1 < 0) {
                                x1 += 360.0;
                            }
                            if (lon2 < 0) {
                                x2 += 360.0;
                            }
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
                } else if (strFormat.equalsIgnoreCase("json") || strFormat.equalsIgnoreCase("ndjson") || strFormat.equalsIgnoreCase("txt")) {

                    JSONObject geom2 = new JSONObject();
                    if (strGeomType.equalsIgnoreCase("geojson")) {
                        geom2.put("coordinates", line);

                    } else {
                        JSONArray path = new JSONArray();
                        path.put(line);
                        geom2.put("paths", path);
                    }

                    if (strFormat.equalsIgnoreCase("json") || strFormat.equalsIgnoreCase("ndjson")) {
                        result.put("name", pos.getName());
                        result.put("num", sat);
                        result.put("geometry", geom2);
                        
                        if (strFormat.equalsIgnoreCase("json")) {
                            results.put(result);
                        } else {
                            out.println(result.toString());
                        }
                        
                        

                    } else {
                        // Default to delimited
                        
                        String strLine = pos.getName() + "|" + pos.getNum() + "|"
                                + geom2.toString() + "";
                        out.println(strLine);

                    }

                } else {
                    throw new Exception("Invalid format. Supported formats: txt,json,geojson");
                }

            }


            if (strFormat.equalsIgnoreCase("geojson")) {
                response.setContentType("application/json;charset=UTF-8");
                JSONObject featureCollection = new JSONObject();
                featureCollection.put("type", "FeatureCollection");

                featureCollection.put("features", results);

                featureCollection.write(out);
                

            } else if (strFormat.equalsIgnoreCase("json")) {
                response.setContentType("application/json;charset=UTF-8");
                results.write(out);
                out.println(results.toString());

            } else {
                response.setContentType("text/plain;charset=UTF-8");                
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

                out.println("<h2>Unexpected Error: " + e.getMessage() + "</h2>");

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
