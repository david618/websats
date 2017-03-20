/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
@WebServlet(name = "tles", urlPatterns = {"/tles"})
public class tles extends HttpServlet {

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
            Sats satDB = new Sats();

            String strFormat = "";
            String strNums = "";
            String strNames = "";

            // Populate the parameters ignoring case
            Enumeration paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                if (paramName.equalsIgnoreCase("f")) {
                    strFormat = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("names")) {
                    strNames = request.getParameter(paramName);
                } else if (paramName.equalsIgnoreCase("nums")) {
                    strNums = request.getParameter(paramName);
                }
            }

            String fmt = "txt";
            if (strFormat.equalsIgnoreCase("txt") || strFormat.equalsIgnoreCase("")) {
                fmt = "txt";
            } else if (strFormat.equalsIgnoreCase("json")) {
                fmt = "json";
            } else {
                throw new Exception("Unsupported Format. Supported formats: txt, json");
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

            // Process the list of satellites
            for (String sat : sats) {
                String tle = satDB.getSatTLE(sat);
                JSONObject result = new JSONObject();

                if (fmt.equalsIgnoreCase("json") || fmt.equalsIgnoreCase("txt")) {


                    if (fmt.equalsIgnoreCase("json")) {
                        String[] tleparts = tle.split("\n");
                        result.put("header", tleparts[0].trim());
                        result.put("line1", tleparts[1].trim());
                        result.put("line2", tleparts[2].trim());
                        results.put(result);

                    } else {
                        // Default to delimited the geom is inside quotes and replace quotes with \"
                        strLines += tle + "\n";

                    }

                } else {
                    throw new Exception("Invalid format. Supported formats: txt,json,geojson");
                }

            }

            PrintWriter out = response.getWriter();

            if (fmt.equalsIgnoreCase("json")) {
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
