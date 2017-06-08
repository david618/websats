package org.jennings.websats;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
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

    private static Sats satDB = null;

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

            String strFormat = "";
            String strGeomType = "";
            String strNums = "";
            String strNames = "";
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

            long t = System.currentTimeMillis();  // Default 1 minute before current time
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
                    throw new Exception("Invalid Geometry Type. Values allowed esri or geojson");
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
                if (numPoints < 20) {
                    numPoints = 20;
                }
                if (numPoints > 500) {
                    numPoints = 500;
                }
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

            int i = 0;
            PrintWriter out = response.getWriter();

            // Process the list of satellites
            for (String sat : sats) {

                i += 1;
                Sat st = satDB.getSatNum(sat);
                Sat pos = st.getPos(t);

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
                } else if (format.equalsIgnoreCase("json") || format.equalsIgnoreCase("ndjson") || format.equalsIgnoreCase("txt")) {

                    JSONObject geom2 = new JSONObject();
                    if (strGeomType.equalsIgnoreCase("geojson")) {
                        geom2.put("coordinates", polys);

                    } else {
                        geom2.put("rings", polys.getJSONArray(0));
                    }

                    if (format.equalsIgnoreCase("json")) {
                        result.put("name", pos.getName());
                        result.put("num", sat);
                        result.put("geometry", geom2);
                        if (format.equalsIgnoreCase("json")) {
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


            if (format.equalsIgnoreCase("geojson")) {
                response.setContentType("application/json;charset=UTF-8");
                JSONObject featureCollection = new JSONObject();
                featureCollection.put("type", "FeatureCollection");

                featureCollection.put("features", results);

                featureCollection.write(out);

            } else if (format.equalsIgnoreCase("json")) {
                response.setContentType("application/json;charset=UTF-8");
                results.write(out);

            } else {
                // Pipe Delimited
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
