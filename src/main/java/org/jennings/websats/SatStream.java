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
@ServerEndpoint(value="/satstream", encoders = {StreamEncoder.class}, decoders = {StreamDecoder.class})
public class SatStream {

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
                        result.put("name", pos.getName());
                        result.put("num", sat);
                        result.put("timestamp", pos.GetEpoch().epochTimeMillis());
                        result.put("dtg", pos.GetEpoch());
                        result.put("lon", pos.GetLon());
                        result.put("lat", pos.GetParametricLat());
                        result.put("alt", pos.getAltitude());
                        broadcast(result.toString());
                        
                        results.put(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }  
                    if (cnt == 100000) {
                        timer.cancel();
                        break;
                    }
                }
                //broadcast(results.toString());
                //broadcast("OK");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    Timer timer;
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
            timer.schedule(new SendSats(), 0, 10);
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
