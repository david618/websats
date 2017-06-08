package org.jennings.websats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jennings.geomtools.GeographicCoordinate;
import org.jennings.geomtools.GreatCircle;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 */
@WebServlet(name = "ellipses", urlPatterns = {"/ellipses"})
public class ellipses extends HttpServlet {

    private static ArrayList<GeographicCoordinate> landGrids = null;
    

    private String loadLandGrids() {
        landGrids = new ArrayList<>();

        String message = "";

        try {
            InputStream pis = getClass().getResourceAsStream("/landgrids.csv");

            BufferedReader br = new BufferedReader(new InputStreamReader(pis, "UTF-8"));
            br.readLine();  // discard first line header
            String strLine = "";
            while ((strLine = br.readLine()) != null) {
                String[] parts = strLine.split(",");
                Double lat = Double.parseDouble(parts[0]);
                Double lon = Double.parseDouble(parts[1]);
                GeographicCoordinate coord = new GeographicCoordinate(lon, lat);
                landGrids.add(coord);
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
        try {
            if (landGrids == null) {
                loadLandGrids();
            }

            String strFormat = "";
            String strGeomType = "";
            String strNumPoints = "";
            String strNum = "";
            boolean isNums = false;

            // Populate the parameters ignoring case
            Enumeration paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                if (paramName.equalsIgnoreCase("f")) {
                    strFormat = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("gt")) {
                    strGeomType = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("npts")) {
                    strNumPoints = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("num")) {
                    strNum = request.getParameter(paramName);
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

            int num = 1;
            if (!strNum.equalsIgnoreCase("")) {
                num = Integer.parseInt(strNum);
                if (num < 1) {
                    num = 1;
                }
                if (num > 5000) {
                    num = 5000;
                }
            }

            JSONArray results = new JSONArray();
            String strLines = "";

            int i = 0;

            Random rnd = new Random();

            int numLandGrids = landGrids.size();

            JSONObject wkid = new JSONObject();
            wkid.put("wkid", 4326);

            PrintWriter out = response.getWriter();
            
            // Process the list of satellites
            while (i < num) {

                i += 1;

                double a = rnd.nextDouble() * 1 + 0.01;  // a from 0.01 to 1.1km
                double b = rnd.nextDouble() * 1 + 0.01;  // b from 0.01 to 1.1km
                double r = rnd.nextDouble() * 360; // Rotation 0 to 360 

                GeographicCoordinate llcorner = landGrids.get(rnd.nextInt(numLandGrids));
                
                double minLon = llcorner.getLon();
                double maxLon = minLon + 1;                
                double lon = minLon + (maxLon - minLon) * rnd.nextDouble();

                double minLat = llcorner.getLat();
                double maxLat = minLat + 1;
                
                double lat = minLat + (maxLat - minLat) * rnd.nextDouble();

                JSONObject result = new JSONObject();
                JSONArray polys = new JSONArray();
                try {

                    // Problem with large areas crossing -180 and 180 for now I'll set to small number
                    GreatCircle gc = new GreatCircle();
                    polys = gc.createEllipse3(lon, lat, a, b, r, numPoints, isClockwise);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (format.equalsIgnoreCase("geojson")) {

                    result.put("type", "Feature");

                    JSONObject properties = new JSONObject();

                    properties.put("a", a);
                    properties.put("b", b);
                    properties.put("rot", r);
                    properties.put("num", i);
                    properties.put("clon", lon);
                    properties.put("clat", lat);
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

                    if (format.equalsIgnoreCase("json") || format.equalsIgnoreCase("ndjson")) {
                        result.put("a", a);
                        result.put("b", b);
                        result.put("rot", r);
                        result.put("num", i);
                        result.put("clon", lon);
                        result.put("clat", lat);
                        JSONObject pt = new JSONObject();
                        pt.put("x", lon);
                        pt.put("y", lat);
                        pt.put("spatialReference",wkid);
			
                        result.put("geompt", pt);
                        result.put("geometry", geom2);
                        results.put(result);
                        
                        if (format.equalsIgnoreCase("json")) {
                            results.put(result);
                        } else {
                            out.println(result.toString());
                        }                   

                    } else {
                        // Default to delimited
                        String strLine = a + "|" + b + "|" + r + "|" + i + "|" + lon + "|" + lat + "|" 
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
                out.println("<h1>Could build ellipses.</h1>");

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
