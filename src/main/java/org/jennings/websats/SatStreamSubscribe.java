package org.jennings.websats;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.jennings.mvnsat.Sat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 */
@ServerEndpoint(value="/SatStream/subscribe", encoders = {StreamEncoder.class}, decoders = {StreamDecoder.class})
public class SatStreamSubscribe {

    static int cnt;
    
    class SendSats extends TimerTask {

        @Override
        public void run() {
            try {
                long t = System.currentTimeMillis();
                JSONArray results = new JSONArray();

                
                
                for (String sat : satDB.getAllNums()) {
                    cnt += 1;
                    try {
                        Sat pos = satDB.getSatNum(sat).getPos(t);
                        
                        JSONObject result = new JSONObject();
                        
                        JSONObject geom = new JSONObject();
//                        geom.put("x", pos.GetLon());
//                        geom.put("y", pos.GetParametricLat());
                        geom.put("x", pos.GetLon() * 20037508.34 / 180);
                        double y = Math.log(Math.tan((90 + pos.GetParametricLat()) * Math.PI / 360)) / (Math.PI / 180);
                        geom.put("y", y * 20037508.34 / 180);
                        
                        JSONObject sr = new JSONObject();
//                        sr.put("wkid", 4326);
                        sr.put("wkid", 102100);
                                                
                        geom.put("spatialReference", sr);
                        
                        result.put("geometry", geom);
                        
                        JSONObject attr = new JSONObject();
                        attr.put("name", pos.getName());
                        attr.put("num", sat);
                        attr.put("timestamp", pos.GetEpoch().epochTimeMillis());
                        attr.put("dtg", pos.GetEpoch());
                        attr.put("lon", pos.GetLon());
                        attr.put("lat", pos.GetParametricLat());
                        attr.put("alt", pos.getAltitude());
                        
                        result.put("attributes", attr);
                        
                        broadcast(result.toString());
                        
                        results.put(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }  
//                    if (cnt == 100000) {
//                        timer.cancel();
//                        break;
//                    }
                }
                //broadcast(results.toString());
                //broadcast("OK");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private static Timer timer;
    private static Sats satDB = null;
    private static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());

    public void broadcast(String message) throws IOException, EncodeException {
        for (Session peer : peers) {
            peer.getBasicRemote().sendObject(message);
        }
    }

    @OnOpen
    public void onOpen(Session peer) {
        cnt = 0;
        peers.add(peer);
        if (satDB == null) {
            satDB = new Sats();
        }

        if (timer == null) {
            timer = new Timer();
            timer.schedule(new SendSats(), 0, 1000);
        }
    }

    @OnClose
    public void onClose(Session peer) {
        peers.remove(peer);
        if (peers.isEmpty()) {
            try {
                timer.cancel();
                timer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
