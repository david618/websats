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
import org.jennings.geomtools.GreatCircle;
import org.jennings.mvnsat.Sat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class satfootprints extends HttpServlet {

    private static HashMap<String, Sat> satNames = null;
    private static HashMap<String, Sat> satNums = null;

    private String loadSats() {
        satNames = new HashMap<>();
        satNums = new HashMap<>();

        String message = null;

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

                if (!(message==null)) {

                    // Failed to load satellite lists
                    throw new Exception(message);
                }
            }

            String strFormat = "";
            String strGeomType = "";
            String strNums = "*";
            String strNames = "*";
            String strTime = "";
            String strNumPoints = "";
            boolean isNums = false;

            // Populate the parameters ignoring case
            Enumeration paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                if (paramName.equalsIgnoreCase("f")) {
                    strFormat = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("gt")) {
                    strGeomType = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("t")) {
                    strTime = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("npts")) {
                    strNumPoints = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("names")) {
                    strNames = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("nums")) {
                    strNums = request.getParameter(paramName);
                    isNums = true;
                }
            }

            String[] satInput;
            ArrayList<String> sats = new ArrayList<>();

            long t = System.currentTimeMillis();  // Default to current system time
            if (!strTime.equalsIgnoreCase("")) {
                t = Long.parseLong(strTime);
                if (t < 10000000000L) {
                    // Assume it seconds convert to ms
                    t = t * 1000;
                }
            }

            boolean isClockwise = true;  // Esri Geoms are clockwise
            if (!strGeomType.equalsIgnoreCase("")) {
                if (strGeomType.equalsIgnoreCase("geojson")) {
                    isClockwise = false;
                } else {
                    message = "Invalid Geometry Type. Values allowed esri or geojson";
                    throw new Exception();
                }
            }

            String format = "txt";
            if (!strFormat.equalsIgnoreCase("")) {
                format = strFormat;
                if (strFormat.equalsIgnoreCase("geojson")) {
                    isClockwise = false;
                }
            }

            int numPoints = 50;
            if (!strNumPoints.equalsIgnoreCase("")) {
                numPoints = Integer.parseInt(strNumPoints);
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

            int i = 0;

            // Process the list of satellites
            for (String sat : sats) {

                i += 1;
                Sat pos = satNums.get(sat).getPos(t);
                JSONObject result = new JSONObject();
                JSONArray polys = new JSONArray();
                try {
                    double r = pos.getRadiusFootprint();
                    // Problem with large areas crossing -180 and 180 for now I'll set to small number

                    GreatCircle gc = new GreatCircle();
                    polys = gc.createCircle(pos.GetLon(), pos.GetParametricLat(), r, numPoints, isClockwise);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                
                
                if (format.equalsIgnoreCase("geojson")) {

                    result.put("type", "Feature");

                    JSONObject properties = new JSONObject();

                    properties.put("name", pos.getName());
                    properties.put("num", sat);
                    result.put("properties", properties);

                    JSONObject geom = new JSONObject();
                    geom.put("type", "MultiPolygon");
                    geom.put("coordinates", polys);
                    result.put("geometry", geom);

                    results.put(result);
                } else if (format.equalsIgnoreCase("json") || format.equalsIgnoreCase("txt")) {

                    JSONObject geom2 = new JSONObject();
                    if (strGeomType.equalsIgnoreCase("geojson")) {
                        geom2.put("coordinates", polys);

                    } else {
                        geom2.put("rings", polys);
                    }

                    if (format.equalsIgnoreCase("json")) {
                        result.put("name", pos.getName());
                        result.put("num", sat);
                        result.put("geometry", geom2);
                        results.put(result);

                    } else {
                        // Default to delimited
                        strLines += pos.getName() + "|" + pos.getNum() + "|"
                                + geom2.toString() + "\n";

                    }

                } else {
                    message = "Invalid format. Supported formats: txt,json,geojson";
                    throw new Exception();
                }

//                if (i == 7) {
//                    break;
//                }
            }

            JSONObject resp = new JSONObject();

            PrintWriter out = response.getWriter();

            if (format.equalsIgnoreCase("geojson")) {
                response.setContentType("application/json;charset=UTF-8");
                JSONObject featureCollection = new JSONObject();
                featureCollection.put("type", "FeatureCollection");

                featureCollection.put("features", results);

                out.println(featureCollection);

            } else if (format.equalsIgnoreCase("json")) {
                response.setContentType("application/json;charset=UTF-8");
                out.println(results.toString());

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
