/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jennings.websats;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 *
 * @author david
 */
@WebServlet(name = "SatStream", urlPatterns = {"/SatStream"})
public class SatStream extends HttpServlet {

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
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {

            String strFormat = "";

            // Populate the parameters ignoring case
            Enumeration paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                if (paramName.equalsIgnoreCase("f")) {
                    strFormat = request.getParameter(paramName);
                }
            }

            
            if (strFormat.equalsIgnoreCase("pjson")) {
                strFormat = "pjson";
            } else {
                strFormat = "json";
            }
            
            JSONObject json = new JSONObject();
            

            json.put("description", JSONObject.NULL);
            json.put("objectIdField", JSONObject.NULL);
            json.put("displayField", "num");
            
            JSONObject json2 = new JSONObject();
            json2.put("trackIdField", JSONObject.NULL);
            json2.put("startTimeField", JSONObject.NULL);
            json2.put("endTimeField", JSONObject.NULL);
            json.put("timeInfo", json2);
                                    
            json.put("geometryType", "esriGeometryPoint");
            json.put("geometryField", "Geometry");
                                                
            json2 = new JSONObject();
            json2.put("wkid", 4326);
            json2.put("latestWkid", 4326);
            json.put("spatialReference", json2);
                                    
            // drawaingInfo
            json2 = new JSONObject();
            
            // renderer
            JSONObject json3 = new JSONObject();
            json3.put("type", "simple");
            json3.put("description", "");
            
            // symbol
            JSONObject json4 = new JSONObject();
            json4.put("type", "esriSMS");
            json4.put("style", "esriSMSCircle");
            json4.append("color", 5);
            json4.append("color", 112);
            json4.append("color", 176);
            json4.append("color", 204);
            json4.put("size", 10);
            json4.put("angle", 10);
            json4.put("xoffset", 10);
            json4.put("yoffset", 10);
            
            // color
            JSONObject json5 = new JSONObject();
            json5.append("color", 255);
            json5.append("color", 255);
            json5.append("color", 255);
            json5.append("color", 255);            
            json5.put("width", 1);      
            json4.put("outline", json5);
            
                              
                        
            json3.put("symbol", json4);
            json2.put("renderer", json3);
            json.put("drawingInfo", json2);
            
            
            
                        
            json2 = new JSONObject();
            json2.put("name","name");
            json2.put("type","esriFieldTypeString");
            json2.put("alias","name");
            json2.put("nullable",true);            
            json.append("fields", json2);

            json2 = new JSONObject();
            json2.put("name","num");
            json2.put("type","esriFieldTypeDouble");
            json2.put("alias","num");
            json2.put("nullable",true);            
            json.append("fields", json2);            

            json2 = new JSONObject();
            json2.put("name","timestamp");
            json2.put("type","esriFieldTypeDouble");
            json2.put("alias","timestamp");
            json2.put("nullable",true);            
            json.append("fields", json2);             
            
            
            json2 = new JSONObject();
            json2.put("name","dtg");
            json2.put("type","esriFieldTypeString");
            json2.put("alias","dtg");
            json2.put("nullable",true);            
            json.append("fields", json2);             
            
            json2 = new JSONObject();
            json2.put("name","lon");
            json2.put("type","esriFieldTypeDouble");
            json2.put("alias","lon");
            json2.put("nullable",true);            
            json.append("fields", json2);                

            json2 = new JSONObject();
            json2.put("name","lat");
            json2.put("type","esriFieldTypeDouble");
            json2.put("alias","lat");
            json2.put("nullable",true);            
            json.append("fields", json2);                

            json2 = new JSONObject();
            json2.put("name","alt");
            json2.put("type","esriFieldTypeDouble");
            json2.put("alias","alt");
            json2.put("nullable",true);            
            json.append("fields", json2);                
            
            json.put("currentVersion", "10.5");
            
            json2 = new JSONObject();
            json2.put("transport", "ws");
            json2.append("urls", request.getRequestURL().replace(0, 4, "ws"));
            
            json.append("streamUrls", json2);
            
            json.put("capabilities", "broadcast,subscribe");
            
            if (strFormat.equalsIgnoreCase("pjson")) {
                out.println(json.toString(2));
            } else {
                out.println(json.toString());
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
