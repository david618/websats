/*
 * (C) Copyright 2017 David Jennings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     David Jennings
 */
package org.jennings.websats;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jennings.geotools.Circle;
import org.jennings.geotools.GeographicCoordinate;
import org.jennings.mvnsat.Sat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * In Development: Output json or delimited txt with point, line(Path for +- 1/2
 * day, and polygon for a specified satellite.
 *
 * @author djennings
 */
@WebServlet(name = "satinfo", urlPatterns = {"/satinfo"})
public class satinfo extends HttpServlet {

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

            String strGeomType = "";
            String strNum = "";

            // Populate the parameters ignoring case
            Enumeration paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                if (paramName.equalsIgnoreCase("gt")) {
                    strGeomType = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("num")) {
                    strNum = request.getParameter(paramName);
                }
            }

            long t = System.currentTimeMillis();  // Default 1 minute before current time

            boolean isClockwise = true;  // Esri Geoms are clockwise
            if (!strGeomType.equalsIgnoreCase("")) {
                if (strGeomType.equalsIgnoreCase("geojson")) {
                    isClockwise = false;
                } else {
                    throw new Exception("Invalid Geometry Type. Values allowed esri or geojson");
                }
            }

            int numPoints = 50;

            HashSet<String> sats = new HashSet<>();

            if (!strNum.equalsIgnoreCase("")) {
                // Nums were specified 
                sats = satDB.getSatsByNum(strNum);
            } else {
                // Get ISS 
                sats = satDB.getSatsByNum("25544");
            }

            JSONArray results = new JSONArray();

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

                    Circle cir = new Circle();
                    polys = cir.createCircle(pos.GetLon(), pos.GetParametricLat(), r, numPoints, isClockwise);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                JSONObject geomFootprint = new JSONObject();
                if (strGeomType.equalsIgnoreCase("geojson")) {
                    geomFootprint.put("coordinates", polys);

                } else {
                    JSONArray rngs = new JSONArray();
                    int numPolys = polys.length();
                    int polyNum = 0;
                    while (polyNum < numPolys) {
                        rngs.put(polys.getJSONArray(polyNum).getJSONArray(0));
                        polyNum++;
                    }
                    geomFootprint.put("rings", rngs);
                }

                long t1 = System.currentTimeMillis();
                long tstart = t1;
                long tduration = 7200; // 7200 seconds or 2 hours
                pos = st.getPos(t1);
                double lat1 = pos.GetParametricLat();
                double lon1 = pos.GetLon();
                JSONArray tracks = new JSONArray();
                JSONArray track = new JSONArray();

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
                            track.put(coord);
                        } else {
                            // Insert with 180
                            JSONArray coord = new JSONArray("[180, " + crossing.getLat() + "]");
                            track.put(coord);
                        }
                        // Insert line and start new line
                        tracks.put(track);
                        track = new JSONArray();

                        if (lon2 < 0) {
                            // Start with -180
                            JSONArray coord = new JSONArray("[-180, " + crossing.getLat() + "]");
                            track.put(coord);
                        } else {
                            // Start with 180
                            JSONArray coord = new JSONArray("[180, " + crossing.getLat() + "]");
                            track.put(coord);
                        }

                    }

                    JSONArray coord = new JSONArray("[" + pos.GetLon() + ", " + pos.GetParametricLat() + "]");
                    track.put(coord);

                    lat1 = lat2;
                    lon1 = lon2;

                    // Step 60 seconds
                    t1 += 60 * 1000;
                }
                tracks.put(track);

                JSONObject geomTrack = new JSONObject();
                if (strGeomType.equalsIgnoreCase("geojson")) {
                    geomTrack.put("coordinates", tracks);

                } else {
                    JSONArray path = new JSONArray();
                    path.put(tracks);
                    geomTrack.put("paths", path);
                }

                result.put("name", pos.getName());
                result.put("num", sat);
                result.put("timestamp", pos.GetEpoch().epochTimeMillis());
                result.put("dtg", pos.GetEpoch());
                result.put("lon", pos.GetLon());
                result.put("lat", pos.GetParametricLat());
                result.put("alt", pos.getAltitude());
                result.put("geomftprt", geomFootprint);
                result.put("geomtrack", geomTrack);

                results.put(result);

            }

            response.setContentType("application/json;charset=UTF-8");
            results.write(out);

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
