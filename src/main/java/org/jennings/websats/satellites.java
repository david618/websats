/*
* To run in Jetty 
* Add to webapps Folder of Jetty
* java -jar start.jar -Djetty.port=9999
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
import org.jennings.mvnsat.Sat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 */
@WebServlet(name = "satellites", urlPatterns = {"/satellites"})
public class satellites extends HttpServlet {

    private static Sats satDB = null;
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if satDB servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            if (satDB == null) {
                satDB = new Sats();
            }
            

            String strFormat = "";
            String strNums = "";
            String strNames = "";
            String strTime = "";
            String strGeomType = "";
            String strDel = "|";
            String strNtimes = "";

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
                } else if (paramName.equalsIgnoreCase("names")) {
                    strNames = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("nums")) {
                    strNums = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("n")) {
                    strNtimes = request.getParameter(paramName);
                }
            }

            String fmt = "txt";
            if (strFormat.equalsIgnoreCase("txt") || strFormat.equalsIgnoreCase("")) {
                fmt = "txt";
            } else if (strFormat.equalsIgnoreCase("ndjson")) {
                fmt = "ndjson";
            } else if (strFormat.equalsIgnoreCase("json")) {
                fmt = "json";
            } else if (strFormat.equalsIgnoreCase("geojson")) {
                fmt = "geojson";
            } else {
                throw new Exception("Unsupported Format. Supported formats: txt, json, ndjson, geojson");
            }

            String geomType = "";
            if (!strGeomType.equalsIgnoreCase("")) {
                if (strGeomType.equalsIgnoreCase("geojson")) {
                    geomType = "geojson";
                } else if (strGeomType.equalsIgnoreCase("esri")) {
                    geomType = "esri";
                } else {
                    throw new Exception("Unsupported Geometry Type. Must be esri or geojson");
                }
            }

            long t = System.currentTimeMillis();
            if (!strTime.equalsIgnoreCase("")) {
                t = Long.parseLong(strTime);
                if (t < 10000000000L) {
                    // Assume it seconds convert to ms
                    t = t * 1000;
                }
            }
            
            int ntimes = 1;
            if (!strNtimes.equalsIgnoreCase("")) {
                try {
                    ntimes = Integer.parseInt(strNtimes);
                    if (ntimes < 1) {
                        ntimes = 1;
                    } else if (ntimes > 5000) {
                        ntimes = 5000;
                    }
                    
                } catch (Exception e) {
                    throw new Exception("n must be an integer");
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
            //String strLines = "";
            
            long st = System.currentTimeMillis();
            
            long et = st;
            
            int n = 0;

            PrintWriter out = response.getWriter();

            
            while (n < ntimes) {                
                long t1 = t + (et - st);
                for (String sat : sats) {
                    Sat pos = satDB.getSatNum(sat).getPos(t1);
                    JSONObject result = new JSONObject();
                    JSONArray line = new JSONArray();
                    JSONArray lines = new JSONArray();

                    if (fmt.equalsIgnoreCase("geojson")) {

                        result.put("type", "Feature");

                        JSONObject properties = new JSONObject();
                        properties.put("name", pos.getName());
                        properties.put("num", pos.getNum());
                        properties.put("timestamp", pos.GetEpoch().epochTimeMillis());
                        properties.put("dtg", pos.GetEpoch());
                        properties.put("lon", pos.GetLon());
                        properties.put("lat", pos.GetParametricLat());
                        properties.put("alt", pos.getAltitude());
                        result.put("properties", properties);

                        JSONObject geom = new JSONObject();
                        geom.put("type", "Point");
                        JSONArray coord = new JSONArray("[" + pos.GetLon() + ", " + pos.GetParametricLat() + "]");
                        geom.put("coordinates", coord);
                        result.put("geometry", geom);

                        results.put(result);
                    } else if (fmt.equalsIgnoreCase("json") || fmt.equalsIgnoreCase("ndjson") ||  fmt.equalsIgnoreCase("txt")) {

                        JSONObject geom2 = new JSONObject();
                        if (geomType.equalsIgnoreCase("geojson")) {
                            JSONArray coord = new JSONArray("[" + pos.GetLon() + ", " + pos.GetParametricLat() + "]");
                            geom2.put("coordinates", coord);
                        } else {

                            geom2.put("x", pos.GetLon());
                            geom2.put("y", pos.GetParametricLat());

                        }

                        if (fmt.equalsIgnoreCase("json") || fmt.equalsIgnoreCase("ndjson")) {
                            result.put("name", pos.getName());
                            result.put("num", sat);
                            result.put("timestamp", pos.GetEpoch().epochTimeMillis());
                            result.put("dtg", pos.GetEpoch());
                            result.put("lon", pos.GetLon());
                            result.put("lat", pos.GetParametricLat());
                            result.put("alt", pos.getAltitude());
                            if (!geomType.equalsIgnoreCase("")) {
                                result.put("geometry", geom2);
                            } 

                            if (fmt.equalsIgnoreCase("json")) {
                                results.put(result);
                            } else {
                                out.println(result.toString());
                            }
                            

                        } else if (geomType.equalsIgnoreCase("")) {
                            // Default is no geom at all just lon,lat
//                            strLines += pos.getName() + strDel + pos.getNum() + strDel + pos.GetEpoch().epochTimeMillis()
//                                    + strDel + pos.GetEpoch() + strDel + pos.GetLon() + strDel + pos.GetParametricLat()
//                                    + strDel + pos.getAltitude() + "\n";
                            String oline = pos.getName() + strDel + pos.getNum() + strDel + pos.GetEpoch().epochTimeMillis()
                                    + strDel + pos.GetEpoch() + strDel + pos.GetLon() + strDel + pos.GetParametricLat()
                                    + strDel + pos.getAltitude() + "";
                            out.println(oline);
                        } else {
                            // Default to delimited the geom is inside quotes and replace quotes with \"
//                            strLines += pos.getName() + strDel + pos.getNum() + strDel + pos.GetEpoch().epochTimeMillis()
//                                    + "\"" + geom2.toString().replace("\"", "\\\"") + "\"" + "\n";
                            String oline = pos.getName() + strDel + pos.getNum() + strDel + pos.GetEpoch().epochTimeMillis()
                                    + "\"" + geom2.toString().replace("\"", "\\\"") + "\"" + "";
                            out.println(oline);

                        }

                    } else {
                        throw new Exception("Invalid format. Supported formats: txt,json,geojson");
                    }

                }                
                n += 1;
                st = System.currentTimeMillis();
            }

            // Process the list of satellites


            JSONObject resp = new JSONObject();

            if (fmt.equalsIgnoreCase("geojson")) {
                response.setContentType("application/json;charset=UTF-8");
                JSONObject featureCollection = new JSONObject();
                featureCollection.put("type", "FeatureCollection");

                featureCollection.put("features", results);

                //out.println(featureCollection);
                featureCollection.write(out);

            } else if (fmt.equalsIgnoreCase("json")) {
                response.setContentType("application/json;charset=UTF-8");

                if (results.length() > 1) {
                    results.write(out);
                    //out.println(results.toString());
                } else {
                    out.println(results.get(0).toString());
                }
                
                

            } else {
                // Pipe Delimited
                response.setContentType("text/plain;charset=UTF-8");
//                out.println(strLines);
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
                out.println("<h2>Error: " + e.getMessage() + "</h2>");
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
     * @throws ServletException if satDB servlet-specific error occurs
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
     * @throws ServletException if satDB servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns satDB short description of the servlet.
     *
     * @return satDB String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
